import {
  Box, Card, Typography, Chip, Button, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, FormControl, InputLabel, Select, MenuItem,
  IconButton, Alert, CircularProgress, Tooltip,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import SendIcon from '@mui/icons-material/Send';
import CheckIcon from '@mui/icons-material/Check';
import CancelIcon from '@mui/icons-material/Cancel';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../api/client';
import type { ApiResponse, SupplierResponse } from '../../types';
import { Layout } from '../../components/layout/Layout';
import { purchaseOrdersApi } from '../../api/purchaseOrders';
import { branchesApi } from '../../api/branches';
import { productsApi } from '../../api/products';
import type { PurchaseOrderResponse } from '../../types';

const statusColor = (s: string): 'default' | 'warning' | 'info' | 'success' | 'error' =>
  ({ DRAFT: 'default', ORDERED: 'warning', PARTIALLY_RECEIVED: 'info', RECEIVED: 'success', CANCELLED: 'error' } as const)[s] ?? 'default';

const fmt = (n: number) => `$${n.toFixed(2)}`;

export const PurchaseOrders = () => {
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [supplierId, setSupplierId] = useState<number | ''>('');
  const [branchId, setBranchId] = useState<number | ''>('');
  const [notes, setNotes] = useState('');
  const [items, setItems] = useState<{ productId: number; productName: string; quantity: number; unitCost: number }[]>([]);
  const [itemProductId, setItemProductId] = useState<number | ''>('');
  const [itemQty, setItemQty] = useState(1);
  const [itemCost, setItemCost] = useState('');
  const [formError, setFormError] = useState('');

  const { data: orders = [], isLoading } = useQuery({ queryKey: ['purchase-orders'], queryFn: purchaseOrdersApi.getAll });
  const { data: branches = [] } = useQuery({ queryKey: ['branches'], queryFn: branchesApi.getActive });
  const { data: products = [] } = useQuery({ queryKey: ['products'], queryFn: productsApi.getAll });
  const { data: suppliers = [] } = useQuery({
    queryKey: ['suppliers'],
    queryFn: () => apiClient.get<ApiResponse<SupplierResponse[]>>('/suppliers').then((r) => r.data.data),
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['purchase-orders'] });

  const createMutation = useMutation({ mutationFn: purchaseOrdersApi.create, onSuccess: () => { invalidate(); setCreateOpen(false); resetForm(); }, onError: (err: unknown) => setFormError((err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Error') });
  const submitMutation = useMutation({ mutationFn: purchaseOrdersApi.submit, onSuccess: invalidate });
  const receiveMutation = useMutation({ mutationFn: purchaseOrdersApi.receive, onSuccess: () => { invalidate(); queryClient.invalidateQueries({ queryKey: ['inventory'] }); } });
  const cancelMutation = useMutation({ mutationFn: purchaseOrdersApi.cancel, onSuccess: invalidate });

  const resetForm = () => { setSupplierId(''); setBranchId(''); setNotes(''); setItems([]); setFormError(''); };

  const addItem = () => {
    if (!itemProductId || itemQty < 1 || !itemCost) return;
    const product = products.find((p) => p.id === itemProductId);
    if (!product) return;
    setItems((prev) => {
      const existing = prev.find((i) => i.productId === itemProductId);
      if (existing) return prev.map((i) => i.productId === itemProductId ? { ...i, quantity: i.quantity + itemQty } : i);
      return [...prev, { productId: product.id, productName: product.name, quantity: itemQty, unitCost: parseFloat(itemCost) }];
    });
    setItemProductId('');
    setItemQty(1);
    setItemCost('');
  };

  const handleCreate = () => {
    if (!supplierId || !branchId) { setFormError('Select supplier and branch'); return; }
    if (items.length === 0) { setFormError('Add at least one item'); return; }
    createMutation.mutate({ supplierId: Number(supplierId), branchId: Number(branchId), notes, items });
  };

  const totalAmount = (order: PurchaseOrderResponse) => order.items.reduce((s, i) => s + i.totalCost, 0);

  const columns: GridColDef<PurchaseOrderResponse>[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'supplierName', headerName: 'Supplier', flex: 1, minWidth: 150 },
    { field: 'branchName', headerName: 'Branch', flex: 1, minWidth: 140 },
    { field: 'status', headerName: 'Status', width: 140, renderCell: ({ value }) => <Chip label={value} color={statusColor(value)} size="small" /> },
    { field: 'totalAmount', headerName: 'Total', width: 110, renderCell: ({ value }) => <Typography fontWeight={700}>{fmt(value ?? 0)}</Typography> },
    { field: 'orderedByName', headerName: 'Created By', flex: 1, minWidth: 140 },
    { field: 'createdAt', headerName: 'Date', flex: 1, minWidth: 160, renderCell: ({ value }) => new Date(value).toLocaleString() },
    {
      field: 'actions', headerName: '', width: 150, sortable: false,
      renderCell: ({ row }) => (
        <Box display="flex" gap={0.5}>
          {row.status === 'DRAFT' && (
            <Tooltip title="Submit to supplier">
              <IconButton size="small" color="warning" onClick={() => submitMutation.mutate(row.id)}><SendIcon fontSize="small" /></IconButton>
            </Tooltip>
          )}
          {(row.status === 'ORDERED' || row.status === 'PARTIALLY_RECEIVED') && (
            <Tooltip title="Mark as received — adds stock">
              <IconButton size="small" color="success" onClick={() => receiveMutation.mutate(row.id)}><CheckIcon fontSize="small" /></IconButton>
            </Tooltip>
          )}
          {row.status !== 'RECEIVED' && row.status !== 'CANCELLED' && (
            <Tooltip title="Cancel order">
              <IconButton size="small" color="error" onClick={() => cancelMutation.mutate(row.id)}><CancelIcon fontSize="small" /></IconButton>
            </Tooltip>
          )}
        </Box>
      ),
    },
  ];

  return (
    <Layout title="Purchase Orders">
      <Box display="flex" justifyContent="flex-end" mb={2}>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>New Order</Button>
      </Box>

      <Card>
        <DataGrid rows={orders} columns={columns} loading={isLoading} autoHeight pageSizeOptions={[20, 50]} initialState={{ pagination: { paginationModel: { pageSize: 20 } } }} disableRowSelectionOnClick sx={{ border: 0 }} />
      </Card>

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle fontWeight={700}>New Purchase Order</DialogTitle>
        <DialogContent>
          {formError && <Alert severity="error" sx={{ mb: 2 }}>{formError}</Alert>}
          <Box display="flex" gap={2} mt={1} mb={2}>
            <FormControl fullWidth size="small">
              <InputLabel>Supplier</InputLabel>
              <Select value={supplierId} label="Supplier" onChange={(e) => setSupplierId(e.target.value as number)}>
                {suppliers.map((s) => <MenuItem key={s.id} value={s.id}>{s.name}</MenuItem>)}
              </Select>
            </FormControl>
            <FormControl fullWidth size="small">
              <InputLabel>Branch</InputLabel>
              <Select value={branchId} label="Branch" onChange={(e) => setBranchId(e.target.value as number)}>
                {branches.map((b) => <MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}
              </Select>
            </FormControl>
          </Box>
          <TextField fullWidth size="small" label="Notes (optional)" value={notes} onChange={(e) => setNotes(e.target.value)} sx={{ mb: 2 }} />

          <Typography variant="subtitle2" fontWeight={700} mb={1}>Items</Typography>
          <Box display="flex" gap={1} mb={1} flexWrap="wrap">
            <FormControl size="small" sx={{ flex: 2, minWidth: 140 }}>
              <InputLabel>Product</InputLabel>
              <Select value={itemProductId} label="Product" onChange={(e) => setItemProductId(e.target.value as number)}>
                {products.map((p) => <MenuItem key={p.id} value={p.id}>{p.name}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField size="small" label="Qty" type="number" value={itemQty} onChange={(e) => setItemQty(Number(e.target.value))} sx={{ width: 70 }} inputProps={{ min: 1 }} />
            <TextField size="small" label="Unit Cost $" type="number" value={itemCost} onChange={(e) => setItemCost(e.target.value)} sx={{ width: 110 }} inputProps={{ min: 0, step: 0.01 }} />
            <Button variant="outlined" onClick={addItem}>Add</Button>
          </Box>

          {items.map((item) => (
            <Box key={item.productId} display="flex" justifyContent="space-between" alignItems="center" py={0.5}>
              <Typography variant="body2">{item.productName}</Typography>
              <Box display="flex" alignItems="center" gap={1}>
                <Chip label={`×${item.quantity} @ $${item.unitCost}`} size="small" />
                <Typography variant="body2" fontWeight={700}>${(item.quantity * item.unitCost).toFixed(2)}</Typography>
                <IconButton size="small" onClick={() => setItems((prev) => prev.filter((i) => i.productId !== item.productId))}><DeleteIcon fontSize="small" /></IconButton>
              </Box>
            </Box>
          ))}
          {items.length > 0 && (
            <Box display="flex" justifyContent="flex-end" mt={1}>
              <Typography fontWeight={700}>Total: ${items.reduce((s, i) => s + i.quantity * i.unitCost, 0).toFixed(2)}</Typography>
            </Box>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => { setCreateOpen(false); resetForm(); }}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate} disabled={createMutation.isPending}>
            {createMutation.isPending ? <CircularProgress size={20} color="inherit" /> : 'Create Order'}
          </Button>
        </DialogActions>
      </Dialog>
    </Layout>
  );
};
