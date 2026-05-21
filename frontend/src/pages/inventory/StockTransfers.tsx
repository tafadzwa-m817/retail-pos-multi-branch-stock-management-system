import {
  Box, Card, Typography, Chip, Button, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, FormControl, InputLabel, Select, MenuItem,
  IconButton, Alert, CircularProgress, Tooltip,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import CheckIcon from '@mui/icons-material/Check';
import DoneAllIcon from '@mui/icons-material/DoneAll';
import CancelIcon from '@mui/icons-material/Cancel';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Layout } from '../../components/layout/Layout';
import { stockTransfersApi } from '../../api/stockTransfers';
import { branchesApi } from '../../api/branches';
import { productsApi } from '../../api/products';
import type { StockTransferResponse } from '../../types';

const statusColor = (s: string): 'warning' | 'info' | 'success' | 'error' | 'default' =>
  ({ PENDING: 'warning', APPROVED: 'info', COMPLETED: 'success', CANCELLED: 'error' } as const)[s] ?? 'default';

export const StockTransfers = () => {
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [fromBranchId, setFromBranchId] = useState<number | ''>('');
  const [toBranchId, setToBranchId] = useState<number | ''>('');
  const [notes, setNotes] = useState('');
  const [items, setItems] = useState<{ productId: number; productName: string; quantity: number }[]>([]);
  const [itemProductId, setItemProductId] = useState<number | ''>('');
  const [itemQty, setItemQty] = useState(1);
  const [formError, setFormError] = useState('');

  const { data: transfers = [], isLoading } = useQuery({
    queryKey: ['stock-transfers'],
    queryFn: stockTransfersApi.getAll,
  });
  const { data: branches = [] } = useQuery({ queryKey: ['branches'], queryFn: branchesApi.getActive });
  const { data: products = [] } = useQuery({ queryKey: ['products'], queryFn: productsApi.getAll });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['stock-transfers'] });

  const createMutation = useMutation({
    mutationFn: stockTransfersApi.create,
    onSuccess: () => { invalidate(); setCreateOpen(false); resetForm(); },
    onError: (err: unknown) => setFormError((err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Error'),
  });
  const approveMutation = useMutation({ mutationFn: stockTransfersApi.approve, onSuccess: invalidate });
  const completeMutation = useMutation({ mutationFn: stockTransfersApi.complete, onSuccess: () => { invalidate(); queryClient.invalidateQueries({ queryKey: ['inventory'] }); } });
  const cancelMutation = useMutation({ mutationFn: stockTransfersApi.cancel, onSuccess: invalidate });

  const resetForm = () => { setFromBranchId(''); setToBranchId(''); setNotes(''); setItems([]); setFormError(''); };

  const addItem = () => {
    if (!itemProductId || itemQty < 1) return;
    const product = products.find((p) => p.id === itemProductId);
    if (!product) return;
    setItems((prev) => {
      const existing = prev.find((i) => i.productId === itemProductId);
      if (existing) return prev.map((i) => i.productId === itemProductId ? { ...i, quantity: i.quantity + itemQty } : i);
      return [...prev, { productId: product.id, productName: product.name, quantity: itemQty }];
    });
    setItemProductId('');
    setItemQty(1);
  };

  const handleCreate = () => {
    if (!fromBranchId || !toBranchId) { setFormError('Select both branches'); return; }
    if (fromBranchId === toBranchId) { setFormError('Source and destination must differ'); return; }
    if (items.length === 0) { setFormError('Add at least one item'); return; }
    createMutation.mutate({ fromBranchId: Number(fromBranchId), toBranchId: Number(toBranchId), notes, items });
  };

  const columns: GridColDef<StockTransferResponse>[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'fromBranchName', headerName: 'From', flex: 1, minWidth: 140 },
    { field: 'toBranchName', headerName: 'To', flex: 1, minWidth: 140 },
    {
      field: 'status', headerName: 'Status', width: 120,
      renderCell: ({ value }) => <Chip label={value} color={statusColor(value)} size="small" />,
    },
    {
      field: 'items', headerName: 'Items', width: 80,
      renderCell: ({ value }) => <Chip label={value?.length ?? 0} size="small" variant="outlined" />,
    },
    { field: 'requestedByName', headerName: 'Requested By', flex: 1, minWidth: 150 },
    {
      field: 'createdAt', headerName: 'Date', flex: 1, minWidth: 160,
      renderCell: ({ value }) => new Date(value).toLocaleString(),
    },
    {
      field: 'actions', headerName: '', width: 150, sortable: false,
      renderCell: ({ row }) => (
        <Box display="flex" gap={0.5}>
          {row.status === 'PENDING' && (
            <Tooltip title="Approve">
              <IconButton size="small" color="info" onClick={() => approveMutation.mutate(row.id)}>
                <CheckIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
          {row.status === 'APPROVED' && (
            <Tooltip title="Complete transfer">
              <IconButton size="small" color="success" onClick={() => completeMutation.mutate(row.id)}>
                <DoneAllIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
          {row.status !== 'COMPLETED' && row.status !== 'CANCELLED' && (
            <Tooltip title="Cancel">
              <IconButton size="small" color="error" onClick={() => cancelMutation.mutate(row.id)}>
                <CancelIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
        </Box>
      ),
    },
  ];

  return (
    <Layout title="Stock Transfers">
      <Box display="flex" justifyContent="flex-end" mb={2}>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
          New Transfer
        </Button>
      </Box>

      <Card>
        <DataGrid
          rows={transfers}
          columns={columns}
          loading={isLoading}
          autoHeight
          pageSizeOptions={[20, 50]}
          initialState={{ pagination: { paginationModel: { pageSize: 20 } } }}
          disableRowSelectionOnClick
          sx={{ border: 0 }}
        />
      </Card>

      {/* Create Dialog */}
      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle fontWeight={700}>New Stock Transfer</DialogTitle>
        <DialogContent>
          {formError && <Alert severity="error" sx={{ mb: 2 }}>{formError}</Alert>}

          <Box display="flex" gap={2} mt={1} mb={2}>
            <FormControl fullWidth size="small">
              <InputLabel>From Branch</InputLabel>
              <Select value={fromBranchId} label="From Branch" onChange={(e) => setFromBranchId(e.target.value as number)}>
                {branches.map((b) => <MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}
              </Select>
            </FormControl>
            <FormControl fullWidth size="small">
              <InputLabel>To Branch</InputLabel>
              <Select value={toBranchId} label="To Branch" onChange={(e) => setToBranchId(e.target.value as number)}>
                {branches.map((b) => <MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}
              </Select>
            </FormControl>
          </Box>

          <TextField fullWidth size="small" label="Notes (optional)" value={notes} onChange={(e) => setNotes(e.target.value)} sx={{ mb: 2 }} />

          <Typography variant="subtitle2" fontWeight={700} mb={1}>Items</Typography>
          <Box display="flex" gap={1} mb={1}>
            <FormControl size="small" sx={{ flex: 1 }}>
              <InputLabel>Product</InputLabel>
              <Select value={itemProductId} label="Product" onChange={(e) => setItemProductId(e.target.value as number)}>
                {products.map((p) => <MenuItem key={p.id} value={p.id}>{p.name}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField size="small" label="Qty" type="number" value={itemQty} onChange={(e) => setItemQty(Number(e.target.value))} sx={{ width: 80 }} inputProps={{ min: 1 }} />
            <Button variant="outlined" onClick={addItem}>Add</Button>
          </Box>

          {items.map((item) => (
            <Box key={item.productId} display="flex" justifyContent="space-between" alignItems="center" py={0.5}>
              <Typography variant="body2">{item.productName}</Typography>
              <Box display="flex" alignItems="center" gap={1}>
                <Chip label={`× ${item.quantity}`} size="small" />
                <IconButton size="small" onClick={() => setItems((prev) => prev.filter((i) => i.productId !== item.productId))}>
                  <DeleteIcon fontSize="small" />
                </IconButton>
              </Box>
            </Box>
          ))}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => { setCreateOpen(false); resetForm(); }}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate} disabled={createMutation.isPending}>
            {createMutation.isPending ? <CircularProgress size={20} color="inherit" /> : 'Request Transfer'}
          </Button>
        </DialogActions>
      </Dialog>
    </Layout>
  );
};
