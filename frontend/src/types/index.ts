// ─── API Wrapper ──────────────────────────────────────────────────────────────

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

// ─── Auth ─────────────────────────────────────────────────────────────────────

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  tokenType: string;
  email: string;
  role: string;
  fullName: string;
  userId: number;
  branchId: number | null;
}

export interface AuthUser {
  userId: number;
  email: string;
  fullName: string;
  role: string;
  branchId: number | null;
}

// ─── Branch ───────────────────────────────────────────────────────────────────

export interface BranchResponse {
  id: number;
  name: string;
  address: string;
  phone: string;
  email: string;
  active: boolean;
  createdAt: string;
}

// ─── Category ─────────────────────────────────────────────────────────────────

export interface CategoryResponse {
  id: number;
  name: string;
  description: string;
}

// ─── Product ──────────────────────────────────────────────────────────────────

export interface ProductResponse {
  id: number;
  name: string;
  sku: string;
  barcode: string;
  description: string;
  categoryId: number;
  categoryName: string;
  costPrice: number;
  sellingPrice: number;
  reorderLevel: number;
  active: boolean;
  createdAt: string;
}

// ─── User ─────────────────────────────────────────────────────────────────────

export interface UserResponse {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  branchId: number | null;
  branchName: string | null;
  active: boolean;
  createdAt: string;
}

// ─── Customer ─────────────────────────────────────────────────────────────────

export interface CustomerResponse {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  address: string;
  loyaltyPoints: number;
  createdAt: string;
}

// ─── Inventory ────────────────────────────────────────────────────────────────

export interface InventoryResponse {
  id: number;
  productId: number;
  productName: string;
  productSku: string;
  branchId: number;
  branchName: string;
  quantity: number;
  reorderLevel: number;
  lowStock: boolean;
  lastUpdated: string;
}

export interface InventoryAdjustRequest {
  productId: number;
  branchId: number;
  quantity: number;
  reason?: string;
}

// ─── Stock Transfer ───────────────────────────────────────────────────────────

export interface StockTransferItem {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
}

export interface StockTransferResponse {
  id: number;
  fromBranchId: number;
  fromBranchName: string;
  toBranchId: number;
  toBranchName: string;
  status: string;
  requestedById: number;
  requestedByName: string;
  approvedById: number | null;
  approvedByName: string | null;
  notes: string;
  items: StockTransferItem[];
  createdAt: string;
  completedAt: string | null;
}

export interface StockTransferRequest {
  fromBranchId: number;
  toBranchId: number;
  notes?: string;
  items: { productId: number; quantity: number }[];
}

// ─── Purchase Order ───────────────────────────────────────────────────────────

export interface PurchaseOrderItem {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
  unitCost: number;
  totalCost: number;
}

export interface PurchaseOrderResponse {
  id: number;
  supplierId: number;
  supplierName: string;
  branchId: number;
  branchName: string;
  status: string;
  orderedById: number;
  orderedByName: string;
  totalAmount: number;
  notes: string;
  items: PurchaseOrderItem[];
  createdAt: string;
  receivedAt: string | null;
}

export interface PurchaseOrderRequest {
  supplierId: number;
  branchId: number;
  notes?: string;
  items: { productId: number; quantity: number; unitCost: number }[];
}

// ─── Sale ─────────────────────────────────────────────────────────────────────

export interface SaleItemResponse {
  id: number;
  productId: number;
  productName: string;
  productSku: string;
  quantity: number;
  unitPrice: number;
  discountAmount: number;
  totalPrice: number;
}

export interface SaleResponse {
  id: number;
  branchId: number;
  branchName: string;
  cashierId: number;
  cashierName: string;
  customerId: number | null;
  customerName: string | null;
  subtotal: number;
  discountAmount: number;
  taxAmount: number;
  totalAmount: number;
  paymentMethod: string;
  paymentReference: string | null;
  status: string;
  notes: string | null;
  items: SaleItemResponse[];
  createdAt: string;
}

export interface SaleItemRequest {
  productId: number;
  quantity: number;
  discountAmount: number;
}

export interface SaleRequest {
  branchId: number;
  customerId?: number | null;
  paymentMethod: string;
  paymentReference?: string;
  loyaltyPointsToRedeem?: number;
  items: SaleItemRequest[];
  notes?: string;
}

export type PaymentMethod = 'CASH' | 'CARD' | 'MOBILE_MONEY' | 'BANK_TRANSFER' | 'CREDIT';

// ─── Cart (local POS state) ───────────────────────────────────────────────────

export interface CartItem {
  product: ProductResponse;
  quantity: number;
  discount: number;
}

// ─── Dashboard ────────────────────────────────────────────────────────────────

export interface BranchPerformance {
  branchId: number;
  branchName: string;
  salesCount: number;
  revenue: number;
}

export interface TopProduct {
  productId: number;
  productName: string;
  quantitySold: number;
  revenue: number;
}

export interface DashboardResponse {
  todaySalesCount: number;
  todayRevenue: number;
  todayAverageOrderValue: number;
  totalActiveProducts: number;
  lowStockItemCount: number;
  totalActiveBranches: number;
  totalActiveCustomers: number;
  totalActiveUsers: number;
  branchPerformance: BranchPerformance[];
  topProductsToday: TopProduct[];
}

// ─── Supplier ─────────────────────────────────────────────────────────────────

export interface SupplierResponse {
  id: number;
  name: string;
  contactPerson: string;
  email: string;
  phone: string;
  address: string;
  active: boolean;
}

// ─── Promotion ────────────────────────────────────────────────────────────────

export interface PromotionResponse {
  id: number;
  name: string;
  description: string;
  discountType: string;
  discountValue: number;
  startDate: string;
  endDate: string;
  minimumPurchaseAmount: number | null;
  applyToAll: boolean;
  applicableProductIds: number[];
  applicableProductNames: string[];
  active: boolean;
  currentlyActive: boolean;
  createdAt: string;
}

// ─── Sales Report ─────────────────────────────────────────────────────────────

export interface ProductSalesSummary {
  productId: number;
  productName: string;
  quantitySold: number;
  revenue: number;
}

export interface SalesReportResponse {
  totalTransactions: number;
  totalRevenue: number;
  averageOrderValue: number;
  branchName: string;
  period: string;
  topProducts: ProductSalesSummary[];
}

// ─── Cash Shift ───────────────────────────────────────────────────────────────

export interface CashShiftResponse {
  id: number;
  branchId: number;
  branchName: string;
  openedById: number;
  openedByName: string;
  closedById?: number;
  closedByName?: string;
  openingFloat: number;
  closingCash?: number;
  totalSalesAmount: number;
  totalTransactions: number;
  variance?: number;
  status: string;
  notes?: string;
  openedAt: string;
  closedAt?: string;
}

// ─── Product Return ───────────────────────────────────────────────────────────

export interface ReturnItemDetail {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
  unitRefundAmount: number;
  totalRefundAmount: number;
}

export interface ReturnResponse {
  id: number;
  originalSaleId: number;
  branchId: number;
  branchName: string;
  processedById: number;
  processedByName: string;
  reason: string;
  totalRefundAmount: number;
  items: ReturnItemDetail[];
  createdAt: string;
}

// ─── Audit Log ────────────────────────────────────────────────────────────────

export interface AuditLogEntry {
  id: number;
  entityType: string;
  entityId?: number;
  action: string;
  performedBy: string;
  details?: string;
  createdAt: string;
}
