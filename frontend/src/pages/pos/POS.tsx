import {
  Box, Grid, TextField, Card, CardContent, Typography, Button, IconButton,
  Divider, MenuItem, Select, FormControl, InputLabel, Chip, Dialog,
  DialogTitle, DialogContent, DialogActions, InputAdornment, Alert,
  CircularProgress, Autocomplete, Avatar, Snackbar, Tooltip,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import AddIcon from '@mui/icons-material/Add';
import RemoveIcon from '@mui/icons-material/Remove';
import DeleteIcon from '@mui/icons-material/Delete';
import PointOfSaleIcon from '@mui/icons-material/PointOfSale';
import PersonIcon from '@mui/icons-material/Person';
import QrCodeScannerIcon from '@mui/icons-material/QrCodeScanner';
import KeyboardIcon from '@mui/icons-material/Keyboard';
import { useState, useCallback, useRef } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { Layout } from '../../components/layout/Layout';
import { Receipt } from '../../components/pos/Receipt';
import { ShortcutHelpModal, useShortcutHelp } from '../../components/pos/ShortcutHelpModal';
import { useBarcodeScanner } from '../../hooks/useBarcodeScanner';
import { usePOSKeyboardShortcuts } from '../../hooks/usePOSKeyboardShortcuts';
import { productsApi } from '../../api/products';
import { branchesApi } from '../../api/branches';
import { customersApi } from '../../api/customers';
import { salesApi } from '../../api/sales';
import { useAuth } from '../../context/AuthContext';
import type { CartItem, CustomerResponse, PaymentMethod, ProductResponse, SaleResponse } from '../../types';

const PAYMENT_METHODS: { value: PaymentMethod; label: string }[] = [
  { value: 'CASH', label: 'Cash' },
  { value: 'CARD', label: 'Card' },
  { value: 'MOBILE_MONEY', label: 'Mobile Money' },
  { value: 'BANK_TRANSFER', label: 'Bank Transfer' },
  { value: 'CREDIT', label: 'Credit' },
];

const fmt = (n: number) => `$${n.toFixed(2)}`;

export const POS = () => {
  const { user } = useAuth();
  const { open: shortcutOpen, close: closeShortcuts } = useShortcutHelp();
  const searchRef = useRef<HTMLInputElement>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [cart, setCart] = useState<CartItem[]>([]);
  const [selectedBranchId, setSelectedBranchId] = useState<number | ''>(user?.branchId ?? '');
  const [selectedCustomer, setSelectedCustomer] = useState<CustomerResponse | null>(null);
  const [customerSearch, setCustomerSearch] = useState('');
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CASH');
  const [paymentRef, setPaymentRef] = useState('');
  const [loyaltyPointsToRedeem, setLoyaltyPointsToRedeem] = useState(0);
  const [paymentOpen, setPaymentOpen] = useState(false);
  const [completedSale, setCompletedSale] = useState<SaleResponse | null>(null);
  const [error, setError] = useState('');
  const [scanToast, setScanToast] = useState('');

  const { data: branches = [] } = useQuery({ queryKey: ['branches'], queryFn: branchesApi.getActive });
  const { data: searchResults = [], isFetching: searching } = useQuery({
    queryKey: ['products', 'search', searchQuery],
    queryFn: () => searchQuery.length >= 2 ? productsApi.search(searchQuery) : productsApi.getAll(),
  });
  const { data: customers = [] } = useQuery({
    queryKey: ['customers', 'search', customerSearch],
    queryFn: () => customerSearch.length >= 2 ? customersApi.search(customerSearch) : Promise.resolve([]),
    enabled: customerSearch.length >= 2,
  });

  const addToCart = useCallback((product: ProductResponse) => {
    setCart((prev) => {
      const ex = prev.find((i) => i.product.id === product.id);
      if (ex) return prev.map((i) => i.product.id === product.id ? { ...i, quantity: i.quantity + 1 } : i);
      return [...prev, { product, quantity: 1, discount: 0 }];
    });
  }, []);

  // Barcode scanner — fires when rapid keyboard input ends with Enter
  const handleBarcodeScan = useCallback(async (barcode: string) => {
    try {
      const product = await productsApi.getByBarcode(barcode);
      addToCart(product);
      setScanToast(`Scanned: ${product.name}`);
    } catch {
      setScanToast(`Barcode not found: ${barcode}`);
    }
  }, [addToCart]);

  useBarcodeScanner(handleBarcodeScan);

  const updateQty = (productId: number, delta: number) =>
    setCart((prev) => prev.map((i) => i.product.id === productId ? { ...i, quantity: Math.max(1, i.quantity + delta) } : i));
  const removeFromCart = (productId: number) =>
    setCart((prev) => prev.filter((i) => i.product.id !== productId));

  const subtotal = cart.reduce((s, i) => s + i.product.sellingPrice * i.quantity, 0);
  const totalDiscount = cart.reduce((s, i) => s + i.discount, 0);
  const total = subtotal - totalDiscount;

  const createSaleMutation = useMutation({
    mutationFn: salesApi.create,
    onSuccess: (data) => { setCompletedSale(data); setCart([]); setSelectedCustomer(null); setPaymentRef(''); setLoyaltyPointsToRedeem(0); setPaymentOpen(false); setError(''); },
    onError: (err: unknown) => setError((err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Sale failed'),
  });

  const handleChargeSale = () => {
    if (!selectedBranchId) { setError('Please select a branch first'); return; }
    if (cart.length === 0) { setError('Cart is empty'); return; }
    setError('');
    setPaymentOpen(true);
  };

  // Register keyboard shortcuts after handleChargeSale is defined
  usePOSKeyboardShortcuts({
    onPaymentMethodChange: setPaymentMethod,
    onCharge: handleChargeSale,
    onClearCart: () => setCart([]),
    onFocusSearch: () => searchRef.current?.focus(),
  });

  const handleConfirmPayment = () => {
    if (!selectedBranchId) return;
    createSaleMutation.mutate({
      branchId: Number(selectedBranchId),
      customerId: selectedCustomer?.id ?? null,
      paymentMethod,
      paymentReference: paymentRef || undefined,
      loyaltyPointsToRedeem: loyaltyPointsToRedeem > 0 ? loyaltyPointsToRedeem : 0,
      items: cart.map((i) => ({ productId: i.product.id, quantity: i.quantity, discountAmount: i.discount })),
    });
  };

  if (completedSale) {
    return (
      <Layout title="POS — Sale Complete">
        <Box display="flex" flexDirection="column" alignItems="center" justifyContent="center" minHeight="50vh">
          <Typography variant="h4" fontWeight={800} mb={1} color="success.main">Sale Complete!</Typography>
          <Typography variant="h5" color="primary.main" fontWeight={700} mb={3}>{fmt(completedSale.totalAmount)}</Typography>
          <Receipt sale={completedSale} />
          <Button variant="contained" size="large" sx={{ mt: 3, px: 5 }}
            onClick={() => { setCompletedSale(null); setSearchQuery(''); }}>
            New Sale
          </Button>
        </Box>
      </Layout>
    );
  }

  return (
    <Layout title="Point of Sale">
      <ShortcutHelpModal open={shortcutOpen} onClose={closeShortcuts} />
      <Snackbar open={!!scanToast} autoHideDuration={2500} onClose={() => setScanToast('')}
        message={scanToast} anchorOrigin={{ vertical: 'top', horizontal: 'center' }} />

      <Grid container spacing={2} sx={{ height: 'calc(100vh - 140px)' }}>
        {/* Left: Products */}
        <Grid item xs={12} md={7} sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
          <Box display="flex" gap={2} mb={2} alignItems="center">
            <FormControl size="small" sx={{ minWidth: 200 }}>
              <InputLabel>Branch</InputLabel>
              <Select value={selectedBranchId} label="Branch" onChange={(e) => setSelectedBranchId(e.target.value as number)}>
                {branches.map((b) => <MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField fullWidth size="small" placeholder="Search by name, SKU or barcode…"
              value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)}
              inputRef={searchRef}
              InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon color="action" /></InputAdornment>, endAdornment: searching ? <CircularProgress size={16} /> : null }} />
            <Chip icon={<QrCodeScannerIcon />} label="Scanner ready" color="success" variant="outlined" size="small" sx={{ whiteSpace: 'nowrap' }} />
            <Tooltip title="Keyboard shortcuts (?)">
              <IconButton size="small" onClick={closeShortcuts}>
                <KeyboardIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          </Box>

          <Box sx={{ flex: 1, overflowY: 'auto' }}>
            <Grid container spacing={1.5}>
              {searchResults.map((product) => (
                <Grid item xs={6} sm={4} lg={3} key={product.id}>
                  <Card onClick={() => addToCart(product)}
                    sx={{ cursor: 'pointer', transition: 'transform .15s, box-shadow .15s', '&:hover': { transform: 'translateY(-2px)', boxShadow: 4 } }}>
                    <CardContent sx={{ p: '12px !important' }}>
                      <Avatar sx={{ bgcolor: 'primary.light', width: 36, height: 36, fontSize: 13, fontWeight: 700, mb: 1 }}>
                        {product.name.charAt(0)}
                      </Avatar>
                      <Typography variant="body2" fontWeight={700} noWrap>{product.name}</Typography>
                      <Typography variant="caption" color="text.secondary" display="block" noWrap>{product.sku ?? product.categoryName}</Typography>
                      {product.barcode && <Typography variant="caption" color="text.disabled" display="block" sx={{ fontSize: 10 }}>{product.barcode}</Typography>}
                      <Typography variant="subtitle2" color="primary.main" fontWeight={800} mt={0.5}>{fmt(product.sellingPrice)}</Typography>
                    </CardContent>
                  </Card>
                </Grid>
              ))}
              {searchResults.length === 0 && !searching && (
                <Grid item xs={12}>
                  <Box textAlign="center" py={6} color="text.secondary">
                    <QrCodeScannerIcon sx={{ fontSize: 48, mb: 1, opacity: 0.3 }} />
                    <Typography>Scan a barcode or type to search</Typography>
                  </Box>
                </Grid>
              )}
            </Grid>
          </Box>
        </Grid>

        {/* Right: Cart */}
        <Grid item xs={12} md={5} sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
          <Card sx={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            <CardContent sx={{ pb: 0, borderBottom: '1px solid #E3EAF2' }}>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                <Typography variant="h6" fontWeight={700}>Cart</Typography>
                {cart.length > 0 && <Chip label={`${cart.length} items`} color="primary" size="small" />}
              </Box>
              <Autocomplete size="small" options={customers} value={selectedCustomer}
                onChange={(_, v) => setSelectedCustomer(v)} onInputChange={(_, v) => setCustomerSearch(v)}
                getOptionLabel={(c) => `${c.firstName} ${c.lastName} — ${c.phone ?? c.email}`}
                renderInput={(params) => (
                  <TextField {...params} placeholder="Search customer (optional)"
                    InputProps={{ ...params.InputProps, startAdornment: <PersonIcon fontSize="small" color="action" sx={{ mr: 0.5 }} /> }} />
                )}
                sx={{ mb: 1.5 }} noOptionsText={customerSearch.length < 2 ? 'Type 2+ chars' : 'No customers found'} />
            </CardContent>

            <Box sx={{ flex: 1, overflowY: 'auto', px: 2, py: 1 }}>
              {cart.length === 0 ? (
                <Box display="flex" flexDirection="column" alignItems="center" justifyContent="center" height="100%" color="text.secondary">
                  <PointOfSaleIcon sx={{ fontSize: 56, opacity: 0.2, mb: 1 }} />
                  <Typography variant="body2">Click a product or scan a barcode</Typography>
                </Box>
              ) : cart.map((item) => (
                <Box key={item.product.id} py={1} borderBottom="1px solid #F0F4F8">
                  <Box display="flex" justifyContent="space-between" alignItems="flex-start">
                    <Box flex={1} mr={1}>
                      <Typography variant="body2" fontWeight={600} noWrap>{item.product.name}</Typography>
                      <Typography variant="caption" color="text.secondary">{fmt(item.product.sellingPrice)} each</Typography>
                    </Box>
                    <Box display="flex" alignItems="center" gap={0.5}>
                      <IconButton size="small" onClick={() => updateQty(item.product.id, -1)}><RemoveIcon fontSize="small" /></IconButton>
                      <Typography variant="body2" fontWeight={700} minWidth={20} textAlign="center">{item.quantity}</Typography>
                      <IconButton size="small" onClick={() => updateQty(item.product.id, 1)}><AddIcon fontSize="small" /></IconButton>
                      <IconButton size="small" color="error" onClick={() => removeFromCart(item.product.id)}><DeleteIcon fontSize="small" /></IconButton>
                    </Box>
                  </Box>
                  <Box display="flex" justifyContent="flex-end">
                    <Typography variant="body2" fontWeight={700} color="primary.main">{fmt(item.product.sellingPrice * item.quantity)}</Typography>
                  </Box>
                </Box>
              ))}
            </Box>

            <CardContent sx={{ borderTop: '1px solid #E3EAF2' }}>
              {error && <Alert severity="error" sx={{ mb: 1.5, py: 0 }}>{error}</Alert>}
              <Box display="flex" justifyContent="space-between" mb={0.5}>
                <Typography color="text.secondary">Subtotal</Typography>
                <Typography fontWeight={600}>{fmt(subtotal)}</Typography>
              </Box>
              {totalDiscount > 0 && (
                <Box display="flex" justifyContent="space-between" mb={0.5}>
                  <Typography color="error.main">Discount</Typography>
                  <Typography color="error.main" fontWeight={600}>-{fmt(totalDiscount)}</Typography>
                </Box>
              )}
              <Divider sx={{ my: 1 }} />
              <Box display="flex" justifyContent="space-between" mb={2}>
                <Typography variant="h6" fontWeight={800}>Total</Typography>
                <Typography variant="h6" fontWeight={800} color="primary.main">{fmt(total)}</Typography>
              </Box>
              <Button fullWidth variant="contained" size="large" startIcon={<PointOfSaleIcon />}
                onClick={handleChargeSale} disabled={cart.length === 0 || createSaleMutation.isPending}
                sx={{ py: 1.5, fontSize: 16, fontWeight: 800 }}>
                {createSaleMutation.isPending ? <CircularProgress size={22} color="inherit" /> : `Charge ${fmt(total)}`}
              </Button>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Payment Dialog */}
      <Dialog open={paymentOpen} onClose={() => setPaymentOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle fontWeight={700}>Complete Payment</DialogTitle>
        <DialogContent>
          <Box display="flex" justifyContent="center" mb={3}>
            <Typography variant="h3" fontWeight={800} color="primary.main">{fmt(total)}</Typography>
          </Box>
          <FormControl fullWidth sx={{ mb: 2 }}>
            <InputLabel>Payment Method</InputLabel>
            <Select value={paymentMethod} label="Payment Method" onChange={(e) => setPaymentMethod(e.target.value as PaymentMethod)}>
              {PAYMENT_METHODS.map((m) => <MenuItem key={m.value} value={m.value}>{m.label}</MenuItem>)}
            </Select>
          </FormControl>
          <TextField fullWidth label="Reference / Receipt No. (optional)" value={paymentRef} onChange={(e) => setPaymentRef(e.target.value)} size="small" />
          {selectedCustomer && (
            <Box mt={2} p={1.5} bgcolor="action.hover" borderRadius={2}>
              <Typography variant="caption" color="text.secondary">Customer</Typography>
              <Typography variant="body2" fontWeight={700}>{selectedCustomer.firstName} {selectedCustomer.lastName}</Typography>
              <Typography variant="caption" color="warning.main">
                Balance: {selectedCustomer.loyaltyPoints} pts (= ${(selectedCustomer.loyaltyPoints * 0.01).toFixed(2)})
              </Typography>
              {selectedCustomer.loyaltyPoints > 0 && (
                <Box mt={1}>
                  <TextField
                    size="small" fullWidth type="number"
                    label={`Redeem points (max ${selectedCustomer.loyaltyPoints})`}
                    value={loyaltyPointsToRedeem}
                    onChange={(e) => {
                      const v = Math.max(0, Math.min(selectedCustomer.loyaltyPoints, Number(e.target.value)));
                      setLoyaltyPointsToRedeem(v);
                    }}
                    inputProps={{ min: 0, max: selectedCustomer.loyaltyPoints, step: 10 }}
                    helperText={loyaltyPointsToRedeem > 0 ? `−$${(loyaltyPointsToRedeem * 0.01).toFixed(2)} discount` : 'Optional — 1 pt = $0.01'}
                  />
                </Box>
              )}
              <Typography variant="caption" color="success.main" display="block" mt={0.5}>
                +{Math.floor(total - loyaltyPointsToRedeem * 0.01)} pts will be earned
              </Typography>
            </Box>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setPaymentOpen(false)} disabled={createSaleMutation.isPending}>Cancel</Button>
          <Button variant="contained" onClick={handleConfirmPayment} disabled={createSaleMutation.isPending} sx={{ px: 4 }}>
            {createSaleMutation.isPending ? <CircularProgress size={20} color="inherit" /> : 'Confirm'}
          </Button>
        </DialogActions>
      </Dialog>
    </Layout>
  );
};
