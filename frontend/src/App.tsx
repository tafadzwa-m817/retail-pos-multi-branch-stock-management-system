import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { CssBaseline } from '@mui/material';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AppThemeProvider } from './context/ThemeContext';
import { AuthProvider } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';
import { ProtectedRoute } from './components/common/ProtectedRoute';
import { Login } from './pages/auth/Login';
import { Dashboard } from './pages/dashboard/Dashboard';
import { POS } from './pages/pos/POS';
import { Inventory } from './pages/inventory/Inventory';
import { StockTransfers } from './pages/inventory/StockTransfers';
import { PurchaseOrders } from './pages/inventory/PurchaseOrders';
import { SalesHistory } from './pages/sales/SalesHistory';
import { Customers } from './pages/customers/Customers';
import { Products } from './pages/products/Products';
import { Reports } from './pages/reports/Reports';
import { Promotions } from './pages/promotions/Promotions';
import { Settings } from './pages/settings/Settings';
import { CashShifts } from './pages/shifts/CashShifts';
import { Returns } from './pages/returns/Returns';
import { Expenses } from './pages/expenses/Expenses';
import { AuditLogs } from './pages/auditlogs/AuditLogs';
import { SupplierCatalog } from './pages/supplier-catalog/SupplierCatalog';
import { Profile } from './pages/profile/Profile';
import { SalesTargets } from './pages/targets/SalesTargets';
import { Wastage } from './pages/wastage/Wastage';
import { CreditAccounts } from './pages/credit/CreditAccounts';

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, staleTime: 30_000 } },
});

const Guard = ({ children }: { children: React.ReactNode }) => (
  <ProtectedRoute>{children}</ProtectedRoute>
);

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AppThemeProvider>
        <CssBaseline />
        <ToastProvider>
          <AuthProvider>
            <BrowserRouter>
              <Routes>
                <Route path="/login" element={<Login />} />
                <Route path="/" element={<Navigate to="/dashboard" replace />} />

                <Route path="/dashboard"        element={<Guard><Dashboard /></Guard>} />
                <Route path="/pos"              element={<Guard><POS /></Guard>} />
                <Route path="/inventory"        element={<Guard><Inventory /></Guard>} />
                <Route path="/transfers"        element={<Guard><StockTransfers /></Guard>} />
                <Route path="/purchase-orders"  element={<Guard><PurchaseOrders /></Guard>} />
                <Route path="/sales"            element={<Guard><SalesHistory /></Guard>} />
                <Route path="/shifts"           element={<Guard><CashShifts /></Guard>} />
                <Route path="/returns"          element={<Guard><Returns /></Guard>} />
                <Route path="/credit-accounts"  element={<Guard><CreditAccounts /></Guard>} />
                <Route path="/customers"        element={<Guard><Customers /></Guard>} />
                <Route path="/promotions"       element={<Guard><Promotions /></Guard>} />
                <Route path="/products"         element={<Guard><Products /></Guard>} />
                <Route path="/supplier-catalog" element={<Guard><SupplierCatalog /></Guard>} />
                <Route path="/reports"          element={<Guard><Reports /></Guard>} />
                <Route path="/targets"          element={<Guard><SalesTargets /></Guard>} />
                <Route path="/expenses"         element={<Guard><Expenses /></Guard>} />
                <Route path="/wastage"          element={<Guard><Wastage /></Guard>} />
                <Route path="/audit-logs"       element={<Guard><AuditLogs /></Guard>} />
                <Route path="/settings"         element={<Guard><Settings /></Guard>} />
                <Route path="/profile"          element={<Guard><Profile /></Guard>} />

                <Route path="*" element={<Navigate to="/dashboard" replace />} />
              </Routes>
            </BrowserRouter>
          </AuthProvider>
        </ToastProvider>
      </AppThemeProvider>
    </QueryClientProvider>
  );
}

export default App;
