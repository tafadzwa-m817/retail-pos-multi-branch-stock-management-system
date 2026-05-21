# CLAUDE.md — Retail POS & Multi-Branch Stock Management

This file gives Claude Code full context on this project so no re-discovery is needed at the start of each session.

---

## Project Identity

- **Package base:** `zw.co.july28.retail`
- **Company:** july28 (denzel@july28.co.zw)
- **Build:** Maven, Java 21, Spring Boot 3.2.5
- **Database:** H2 in-memory (`jdbc:h2:mem:retaildb`, user `sa`, password `password`)
- **Source files:** 125 Java files across 8 packages
- **Port:** 8080 (use `--server.port=8081` if occupied)

---

## How to Run

```bash
mvn spring-boot:run
# or on a specific port
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- H2 Console: http://localhost:8080/h2-console

---

## Architecture Overview

```
controller → service → repository → H2
                ↘ (cross-service calls for inventory, audit, promotions)
security: JwtAuthenticationFilter → JwtTokenProvider → UserDetailsServiceImpl
```

All controllers return `ApiResponse<T>`. Paginated endpoints return `ApiResponse<PageResponse<T>>`.

---

## Package Map

| Package | Contents |
|---|---|
| `config` | SecurityConfig, OpenApiConfig, DataInitializer (seed data on startup) |
| `controller` | 17 controllers — one per domain module |
| `dto.request` | Validated request bodies |
| `dto.response` | Response DTOs — entities are never serialised directly |
| `entity` | 15 JPA entities — all use Lombok @Builder |
| `enums` | Role, TransferStatus, OrderStatus, SaleStatus, PaymentMethod, ShiftStatus, DiscountType, AuditAction |
| `exception` | GlobalExceptionHandler (@RestControllerAdvice), ResourceNotFoundException, BadRequestException, InsufficientStockException |
| `repository` | 15 Spring Data JPA repos |
| `security` | JwtTokenProvider (jjwt 0.12.3), JwtAuthenticationFilter, UserDetailsServiceImpl |
| `service` | 12 service classes — all business logic lives here |

---

## Key Entities & Relationships

```
AppUser  → Branch (ManyToOne, nullable for SUPER_ADMIN)
Product  → Category (ManyToOne)
Inventory → Product + Branch (unique constraint, one record per pair)
Sale     → Branch, AppUser (cashier), Customer (nullable)
SaleItem → Sale, Product
StockTransfer → fromBranch, toBranch, requestedBy, approvedBy
StockTransferItem → StockTransfer, Product
PurchaseOrder → Supplier, Branch, orderedBy
PurchaseOrderItem → PurchaseOrder, Product
ProductReturn → originalSale, Branch, processedBy
ReturnItem → ProductReturn, SaleItem (original), Product
CashShift → Branch, openedBy, closedBy
Promotion → Product (ManyToMany via promotion_products)
RefreshToken → AppUser
AuditLog  → standalone (entityType, entityId, action, performedBy)
```

`@JsonIgnore` on all back-references (SaleItem.sale, StockTransferItem.stockTransfer, etc.) — serialisation always goes through response DTOs.

---

## Auth Flow

- **Login/Register** → returns `{ token (1h), refreshToken (7d) }`
- **Refresh** → `POST /api/auth/refresh { refreshToken }` → new token pair
- **Logout** → `POST /api/auth/logout { refreshToken }` → revokes token
- `RefreshTokenRepository.revokeAllUserTokens(userId)` runs on every new refresh, so only one active refresh token per user at a time
- `JwtTokenProvider` uses `Decoders.BASE64.decode(jwtSecret)` + `Keys.hmacShaKeyFor()`
- JWT claims include: `sub` (email), `role`, `userId`, `name`

---

## Business Rules — Critical Domain Logic

### Sales (SaleService.createSale)
1. Load branch, cashier, customer (optional)
2. Load all active promotions (`PromotionService.findCurrentlyActivePromotions()`)
3. For each item: check stock level → throw `InsufficientStockException` if short
4. Calculate: `promotionDiscount` (PERCENTAGE or FIXED_AMOUNT, stacks with manual discount)
5. Persist `Sale` + `SaleItem` records
6. Deduct inventory for each item (`InventoryService.deductStock`)
7. Award loyalty points to customer if present (1 pt per dollar)
8. Write audit log entry

### Stock Transfers (StockTransferService)
- Flow: `PENDING → APPROVED → COMPLETED` (or `CANCELLED` at any non-completed state)
- `completeTransfer`: checks all source stock levels first, then atomically deducts source + adds destination
- Source and destination branches must be different

### Purchase Orders (PurchaseOrderService)
- Flow: `DRAFT → ORDERED → RECEIVED` (or `CANCELLED` before RECEIVED)
- `receiveOrder`: adds each item's quantity to the branch inventory via `InventoryService.addStock`

### Returns (ReturnService.processReturn)
- Validates: sale exists, not voided, each return item belongs to the sale, qty ≤ (sold - already returned)
- Restores inventory for each returned item
- Marks original sale as `REFUNDED`
- Refund amount = unitPrice − (itemDiscount / qty) per unit × qty returned

### Cash Shifts (CashShiftService)
- Only one `OPEN` shift allowed per branch at a time
- `closeShift`: queries all sales for the branch between `openedAt` and `closedAt`, sums `totalAmount`
- `variance = closingCash - openingFloat - totalSalesAmount` (calculated in `CashShiftResponse.from`)

### Inventory
- `InventoryService.getOrCreate`: finds or creates a zero-quantity record for product+branch
- `deductStock` / `addStock` use `getOrCreate` internally — safe for branches with no prior inventory record
- Manual `adjustInventory` writes an audit log entry

---

## Roles & Security

```java
// Defined in SecurityConfig — these paths are public:
"/api/auth/**", "/h2-console/**", "/swagger-ui/**", "/v3/api-docs/**"

// Method-level security uses @PreAuthorize, e.g.:
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
// Role prefix is "ROLE_" (set in AppUser.getAuthorities())
```

Roles in order of privilege: `SUPER_ADMIN > ADMIN > BRANCH_MANAGER > CASHIER`

---

## Audit Logging

`AuditLogService.log(entityType, entityId, action, details)` — call from service methods.  
Resolves current user from `SecurityContextHolder` (falls back to `"system"` if no auth context).

Currently wired in:
- `AuthService` — LOGIN, LOGOUT, CREATE (register)
- `UserService` — CREATE, UPDATE, DELETE
- `SaleService` — CREATE, VOID
- `InventoryService` — ADJUST
- `ReturnService` — CREATE

To add audit logging to any other service, inject `AuditLogService` and call `.log(...)`.

**Important:** `InventoryService` uses `@Lazy AuditLogService` via a manual constructor to break a potential circular dependency with services that depend on both.

---

## Pagination Pattern

Services that support pagination return `PageResponse<T>`:
```java
// In service:
public PageResponse<SaleResponse> getAllSales(Pageable pageable) {
    return PageResponse.from(saleRepository.findAll(pageable).map(SaleResponse::from));
}

// In controller:
Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
return ResponseEntity.ok(ApiResponse.ok(saleService.getAllSales(pageable)));
```

`PageResponse<T>` has: `content, page, size, totalElements, totalPages, first, last`

---

## Seed Data (DataInitializer)

Runs on startup only when `userRepository.count() == 0`.

Creates:
- 3 branches: Main (Harare CBD), North (Borrowdale), Bulawayo
- 3 users: super admin (no branch), manager (Main), cashier (Main)
- 4 categories: Electronics, Clothing, Food & Beverages, Household
- 2 suppliers: TechZim Distributors, Fresh Foods Ltd
- 10 products with realistic Zimbabwean retail pricing (USD)
- Inventory: 100 @ Main, 50 @ Borrowdale, 30 @ Bulawayo for every product

---

## Common Pitfalls to Avoid

1. **Never expose entities directly from controllers** — always map to a response DTO.
2. **H2 keyword conflict** — `user` is a reserved H2 keyword; the entity table is named `app_users`.
3. **`NON_KEYWORDS=USER`** is set in the datasource URL as a backup safety.
4. **`spring.jpa.hibernate.ddl-auto=create-drop`** — schema drops on shutdown. Switch to `update` or `validate` for persistence.
5. **Circular dependency** — `InventoryService` ↔ `AuditLogService` was broken with `@Lazy`. Replicate this pattern if adding new cross-dependencies.
6. **`RefreshToken.revokeAllUserTokens`** uses a `@Modifying` JPQL query — always call inside a `@Transactional` method.
7. **`StockTransfer.completeTransfer`** is the only place that moves stock between branches — do not duplicate this logic elsewhere.

---

## Adding a New Module — Checklist

1. Create entity in `entity/` — use `@Builder`, `@Getter`, `@Setter`, `@PrePersist`
2. Create repository in `repository/` — extend `JpaRepository<Entity, Long>`
3. Create request DTO in `dto/request/` — use Jakarta validation annotations
4. Create response DTO in `dto/response/` — add a static `from(Entity)` factory method
5. Create service in `service/` — inject `AuditLogService` if the operation is sensitive
6. Create controller in `controller/` — annotate with `@SecurityRequirement(name = "bearerAuth")` and `@Tag`
7. Add `@PreAuthorize` to restrict endpoints by role
