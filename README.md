# Retail POS & Multi-Branch Stock Management System

A full-featured backend REST API for point-of-sale operations and inventory management across multiple retail branches.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2.5 |
| Language | Java 21 |
| Build | Maven |
| Database | H2 (in-memory) |
| Auth | JWT (access 1h) + Refresh Token (7d) |
| Security | Spring Security with `@EnableMethodSecurity` |
| API Docs | Springdoc OpenAPI (Swagger UI) |
| Utilities | Lombok |

---

## Quick Start

```bash
mvn spring-boot:run
```

| URL | Purpose |
|---|---|
| http://localhost:8080/swagger-ui/index.html | Interactive API docs |
| http://localhost:8080/h2-console | Database browser |

**H2 Console credentials:** JDBC URL `jdbc:h2:mem:retaildb`, Username `sa`, Password `password`

If port 8080 is in use:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

---

## Default Seed Accounts

| Email | Password | Role |
|---|---|---|
| admin@july28retail.co.zw | admin123 | SUPER_ADMIN |
| manager@july28retail.co.zw | manager123 | BRANCH_MANAGER |
| cashier@july28retail.co.zw | cashier123 | CASHIER |

**Seeded branches:** Main Branch (Harare CBD), North Branch (Borrowdale), Bulawayo Branch  
**Seeded products:** 10 products across Electronics, Clothing, Food & Beverages, Household  
**Seeded inventory:** 100 units per product at Main, 50 at Borrowdale, 30 at Bulawayo

---

## Authentication Flow

```
POST /api/auth/login        → { token, refreshToken, role, userId, branchId }
POST /api/auth/register     → { token, refreshToken, ... }
POST /api/auth/refresh      → exchange refreshToken for new access token
POST /api/auth/logout       → revoke refreshToken
```

All protected endpoints require: `Authorization: Bearer <token>`

---

## Role Hierarchy & Permissions

| Role | Access |
|---|---|
| `SUPER_ADMIN` | Everything |
| `ADMIN` | Everything except super-admin operations |
| `BRANCH_MANAGER` | Branch operations, stock transfers, purchase orders, reports |
| `CASHIER` | Process sales, returns, view inventory |

---

## API Modules

### Branches — `/api/branches`
| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/` | Any | List all branches |
| GET | `/active` | Any | List active branches only |
| GET | `/{id}` | Any | Get branch by ID |
| POST | `/` | ADMIN+ | Create branch |
| PUT | `/{id}` | ADMIN+ | Update branch |
| DELETE | `/{id}` | ADMIN+ | Deactivate branch |

### Users — `/api/users`
| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/` | ADMIN+ | List all users |
| GET | `/{id}` | Any | Get user by ID |
| GET | `/branch/{branchId}` | Any | Users in a branch |
| POST | `/` | ADMIN+ | Create user |
| PUT | `/{id}` | ADMIN+ | Update user |
| DELETE | `/{id}` | ADMIN+ | Deactivate user |

### Products — `/api/products`
| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/` | Any | Active products |
| GET | `/all` | ADMIN+ | All including inactive |
| GET | `/{id}` | Any | Product by ID |
| GET | `/search?q=` | Any | Search by name/SKU/barcode |
| GET | `/category/{id}` | Any | Products by category |
| POST | `/` | MANAGER+ | Create product |
| PUT | `/{id}` | MANAGER+ | Update product |
| DELETE | `/{id}` | ADMIN+ | Deactivate product |

### Categories — `/api/categories`
CRUD. Create/update requires BRANCH_MANAGER+. Delete requires ADMIN+.

### Suppliers — `/api/suppliers`
CRUD. Create/update requires BRANCH_MANAGER+.

### Inventory — `/api/inventory`
| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/` | Any | All inventory |
| GET | `/branch/{branchId}` | Any | Inventory for a branch |
| GET | `/low-stock?branchId=` | Any | Items at or below reorder level |
| POST | `/adjust` | MANAGER+ | Manual stock quantity adjustment |

### Stock Transfers — `/api/stock-transfers`
| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/` | Any | All transfers |
| GET | `/{id}` | Any | Transfer by ID |
| GET | `/branch/{branchId}` | Any | Transfers involving a branch |
| POST | `/` | Any | Request a new transfer |
| PUT | `/{id}/approve` | MANAGER+ | Approve pending transfer |
| PUT | `/{id}/complete` | MANAGER+ | Complete — moves stock between branches |
| PUT | `/{id}/cancel` | MANAGER+ | Cancel transfer |

**Transfer flow:** `PENDING → APPROVED → COMPLETED`  
Completing a transfer deducts from the source branch and adds to the destination.

### Purchase Orders — `/api/purchase-orders`
| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/` | Any | All orders |
| POST | `/` | MANAGER+ | Create draft order |
| PUT | `/{id}/submit` | MANAGER+ | Submit to supplier |
| PUT | `/{id}/receive` | MANAGER+ | Receive goods — adds stock to branch |
| PUT | `/{id}/cancel` | ADMIN+ | Cancel order |

**Order flow:** `DRAFT → ORDERED → RECEIVED`  
Receiving a purchase order adds all item quantities to the branch inventory.

### Sales / POS — `/api/sales`
| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/?page=0&size=20` | MANAGER+ | Paginated sales list |
| GET | `/{id}` | Any | Sale by ID |
| GET | `/branch/{branchId}` | Any | Sales by branch |
| GET | `/date-range?start=&end=` | Any | Filter by date range |
| POST | `/` | Any | Process a sale |
| PUT | `/{id}/void` | MANAGER+ | Void sale — restores inventory |

**Sale request body:**
```json
{
  "branchId": 1,
  "customerId": null,
  "paymentMethod": "CASH",
  "items": [
    { "productId": 1, "quantity": 2, "discountAmount": 0.00 }
  ]
}
```

Payment methods: `CASH`, `CARD`, `MOBILE_MONEY`, `BANK_TRANSFER`, `CREDIT`

### Customers — `/api/customers`
| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/` | Any | All customers |
| GET | `/{id}` | Any | Customer by ID |
| GET | `/search?q=` | Any | Search by name/email/phone |
| GET | `/{id}/purchases` | Any | Customer purchase history |
| POST | `/` | Any | Create customer |
| PUT | `/{id}` | Any | Update customer |
| DELETE | `/{id}` | ADMIN+ | Delete customer |

Customers earn **1 loyalty point per dollar** spent.

### Promotions — `/api/promotions`
| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/` | Any | All promotions |
| GET | `/active` | Any | Currently active promotions |
| GET | `/{id}` | Any | Promotion by ID |
| POST | `/` | ADMIN+ | Create promotion |
| PUT | `/{id}` | ADMIN+ | Update promotion |
| DELETE | `/{id}` | ADMIN+ | Delete promotion |

Promotions auto-apply at POS. Types: `PERCENTAGE` or `FIXED_AMOUNT`.  
Can apply to all products (`applyToAll: true`) or a list of specific product IDs.

### Cash Shifts — `/api/shifts`
| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/branch/{branchId}` | Any | Shift history for branch |
| GET | `/{id}` | Any | Shift by ID |
| GET | `/branch/{branchId}/current` | Any | Currently open shift |
| POST | `/open` | Any | Open shift with float amount |
| PUT | `/{id}/close` | Any | Close shift with closing cash |

Closing a shift auto-calculates:
- `totalSalesAmount` = sum of all sales during shift period
- `variance` = closingCash − openingFloat − totalSalesAmount

### Product Returns — `/api/returns`
| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/` | MANAGER+ | All returns |
| GET | `/{id}` | Any | Return by ID |
| GET | `/sale/{saleId}` | Any | Returns for a sale |
| GET | `/branch/{branchId}` | Any | Returns by branch |
| POST | `/` | Any | Process a return |

**Return request body:**
```json
{
  "originalSaleId": 1,
  "reason": "Defective item",
  "items": [
    { "saleItemId": 1, "quantity": 1 }
  ]
}
```

Processing a return restores inventory and marks the original sale as `REFUNDED`.

### Dashboard — `/api/dashboard`
| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/` | MANAGER+ | Today's business summary |

Returns: today's sales count, revenue, avg order value, low stock count, per-branch performance, top 5 products.

### Reports — `/api/reports`
| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/sales?branchId=&start=&end=` | MANAGER+ | Sales report with top products |
| GET | `/inventory/{branchId}` | MANAGER+ | Full inventory for a branch |
| GET | `/low-stock?branchId=` | MANAGER+ | Low stock alerts |
| GET | `/sales/export?start=&end=` | MANAGER+ | Download sales CSV |
| GET | `/inventory/export?branchId=` | MANAGER+ | Download inventory CSV |

Date format for query params: `ISO_DATE_TIME` e.g. `2026-01-01T00:00:00`

### Audit Logs — `/api/audit-logs`
| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/?page=0&size=20` | ADMIN+ | All logs paginated |
| GET | `/entity/{type}?page=` | ADMIN+ | Filter by entity (Sale, AppUser, etc.) |
| GET | `/user/{email}?page=` | ADMIN+ | Filter by user |
| GET | `/entity/{type}/{id}` | ADMIN+ | Full history of a specific record |
| GET | `/date-range?start=&end=` | ADMIN+ | Filter by date range |

Automatically logs: login, logout, user CRUD, sale create/void, stock adjustments, returns.

---

## Domain Entities

```
Branch
  └── AppUser (ManyToOne → Branch)
  └── Inventory (ManyToOne → Branch, ManyToOne → Product) [unique per branch+product]
  └── CashShift (ManyToOne → Branch)
  └── Sale (ManyToOne → Branch)
  └── PurchaseOrder (ManyToOne → Branch)

Category
  └── Product (ManyToOne → Category)

Product
  └── Inventory
  └── SaleItem (ManyToOne → Product)
  └── StockTransferItem
  └── PurchaseOrderItem
  └── Promotion (ManyToMany)

Sale
  └── SaleItem (OneToMany, cascade)
  └── ProductReturn (ManyToOne → Sale)

StockTransfer
  └── StockTransferItem (OneToMany, cascade)

PurchaseOrder
  └── PurchaseOrderItem (OneToMany, cascade)

ProductReturn
  └── ReturnItem (OneToMany, cascade)

Customer (standalone, linked to Sale)
Supplier (standalone, linked to PurchaseOrder)
RefreshToken (ManyToOne → AppUser)
AuditLog (standalone)
```

---

## Pagination

List endpoints that support pagination return:
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false
  }
}
```

---

## Standard API Response

All endpoints return:
```json
{
  "success": true,
  "message": "Success",
  "data": { ... }
}
```

Errors return:
```json
{
  "success": false,
  "message": "Descriptive error message",
  "data": null
}
```

---

## Project Structure

```
src/main/java/zw/co/july28/retail/
├── config/           SecurityConfig, OpenApiConfig, DataInitializer
├── controller/       17 REST controllers
├── dto/
│   ├── request/      LoginRequest, SaleRequest, ReturnRequest, etc.
│   └── response/     ApiResponse<T>, PageResponse<T>, SaleResponse, etc.
├── entity/           15 JPA entities
├── enums/            Role, TransferStatus, OrderStatus, SaleStatus,
│                     PaymentMethod, ShiftStatus, DiscountType, AuditAction
├── exception/        GlobalExceptionHandler, ResourceNotFoundException,
│                     BadRequestException, InsufficientStockException
├── repository/       15 Spring Data JPA repositories
├── security/         JwtTokenProvider, JwtAuthenticationFilter,
│                     UserDetailsServiceImpl
└── service/          12 service classes
```

---

## Environment Configuration

```properties
# application.properties key values
spring.datasource.url=jdbc:h2:mem:retaildb;NON_KEYWORDS=USER
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=create-drop   # tables recreated on restart
jwt.expiration=3600000                       # access token: 1 hour
jwt.refresh-expiration=604800000             # refresh token: 7 days
```

> **Note:** H2 is in-memory — all data resets on restart. To persist data, switch to PostgreSQL or MySQL by updating the datasource properties and adding the relevant driver dependency.
