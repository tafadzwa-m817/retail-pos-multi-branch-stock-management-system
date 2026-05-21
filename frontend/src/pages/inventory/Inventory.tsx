import {
  Box, Card, CardContent, Typography, FormControl, InputLabel, Select,
  MenuItem, Chip, Button, Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, CircularProgress, Alert, Switch, FormControlLabel, Tooltip,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import TuneIcon from '@mui/icons-material/Tune';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Layout } from '../../components/layout/Layout';
import { inventoryApi } from '../../api/inventory';
import { branchesApi } from '../../api/branches';
import type { InventoryResponse } from '../../types';

export const Inventory = () => {
  const queryClient = useQueryClient();
  const [selectedBranchId, setSelectedBranchId] = useState<number | ''>('');
  const [lowStockOnly, setLowStockOnly] = useState(false);
  const [adjustOpen, setAdjustOpen] = useState(false);
  const [adjustItem, setAdjustItem] = useState<InventoryResponse | null>(null);
  const [newQty, setNewQty] = useState('');
  const [adjustReason, setAdjustReason] = useState('');
  const [adjustError, setAdjustError] = useState('');

  const { data: branches = [] } = useQuery({ queryKey: ['branches'], queryFn: branchesApi.getActive });

  const { data: inventory = [], isLoading } = useQuery({
    queryKey: ['inventory', selectedBranchId, lowStockOnly],
    queryFn: () => {
      if (lowStockOnly) return inventoryApi.getLowStock(selectedBranchId || undefined);
      if (selectedBranchId) return inventoryApi.getByBranch(selectedBranchId);
      return inventoryApi.getAll();
    },
  });

  const adjustMutation = useMutation({
    mutationFn: inventoryApi.adjust,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['inventory'] });
      setAdjustOpen(false);
      setAdjustItem(null);
      setNewQty('');
      setAdjustReason('');
      setAdjustError('');
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Adjustment failed';
      setAdjustError(msg);
    },
  });

  const openAdjust = (item: InventoryResponse) => {
    setAdjustItem(item);
    setNewQty(String(item.quantity));
    setAdjustError('');
    setAdjustOpen(true);
  };

  const handleAdjust = () => {
    if (!adjustItem) return;
    const qty = parseInt(newQty, 10);
    if (isNaN(qty) || qty < 0) { setAdjustError('Enter a valid quantity'); return; }
    adjustMutation.mutate({ productId: adjustItem.productId, branchId: adjustItem.branchId, quantity: qty, reason: adjustReason });
  };

  const columns: GridColDef<InventoryResponse>[] = [
    { field: 'productName', headerName: 'Product', flex: 1.5, minWidth: 180 },
    { field: 'productSku', headerName: 'SKU', flex: 0.8, minWidth: 100 },
    {
      field: 'branchName', headerName: 'Branch', flex: 1, minWidth: 140,
      renderCell: ({ value }) => <Chip label={value} size="small" variant="outlined" />,
    },
    {
      field: 'quantity', headerName: 'In Stock', width: 110, type: 'number',
      renderCell: ({ row }) => (
        <Typography
          fontWeight={700}
          color={row.lowStock ? 'error.main' : 'success.main'}
          variant="body2"
        >
          {row.quantity}
        </Typography>
      ),
    },
    { field: 'reorderLevel', headerName: 'Reorder At', width: 110, type: 'number' },
    {
      field: 'lowStock', headerName: 'Status', width: 120,
      renderCell: ({ value }) =>
        value ? (
          <Chip icon={<WarningAmberIcon />} label="Low Stock" color="error" size="small" />
        ) : (
          <Chip label="OK" color="success" size="small" />
        ),
    },
    {
      field: 'lastUpdated', headerName: 'Last Updated', flex: 1, minWidth: 160,
      renderCell: ({ value }) => value ? new Date(value).toLocaleString() : '—',
    },
    {
      field: 'actions', headerName: '', width: 110, sortable: false,
      renderCell: ({ row }) => (
        <Tooltip title="Adjust stock quantity">
          <Button size="small" startIcon={<TuneIcon />} onClick={() => openAdjust(row)}>
            Adjust
          </Button>
        </Tooltip>
      ),
    },
  ];

  const lowStockCount = inventory.filter((i) => i.lowStock).length;

  return (
    <Layout title="Inventory">
      <Box display="flex" gap={2} mb={2} flexWrap="wrap" alignItems="center">
        <FormControl size="small" sx={{ minWidth: 220 }}>
          <InputLabel>Branch</InputLabel>
          <Select value={selectedBranchId} label="Branch" onChange={(e) => setSelectedBranchId(e.target.value as number | '')}>
            <MenuItem value="">All Branches</MenuItem>
            {branches.map((b) => (
              <MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>
            ))}
          </Select>
        </FormControl>

        <FormControlLabel
          control={<Switch checked={lowStockOnly} onChange={(e) => setLowStockOnly(e.target.checked)} color="error" />}
          label={
            <Box display="flex" alignItems="center" gap={0.5}>
              <WarningAmberIcon fontSize="small" color="error" />
              <Typography variant="body2">Low stock only</Typography>
            </Box>
          }
        />

        {lowStockCount > 0 && (
          <Chip
            icon={<WarningAmberIcon />}
            label={`${lowStockCount} low stock item${lowStockCount > 1 ? 's' : ''}`}
            color="error"
            variant="outlined"
          />
        )}

        <Box flex={1} />
        <Typography variant="body2" color="text.secondary">
          {inventory.length} record{inventory.length !== 1 ? 's' : ''}
        </Typography>
      </Box>

      <Card>
        <DataGrid
          rows={inventory}
          columns={columns}
          loading={isLoading}
          autoHeight
          pageSizeOptions={[25, 50, 100]}
          initialState={{ pagination: { paginationModel: { pageSize: 25 } } }}
          disableRowSelectionOnClick
          sx={{ border: 0, '& .MuiDataGrid-columnHeaders': { bgcolor: '#F0F4F8', fontWeight: 700 } }}
        />
      </Card>

      {/* Adjust Dialog */}
      <Dialog open={adjustOpen} onClose={() => setAdjustOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle fontWeight={700}>Adjust Stock</DialogTitle>
        <DialogContent>
          {adjustItem && (
            <Box mb={2} p={1.5} bgcolor="#F0F4F8" borderRadius={2}>
              <Typography variant="subtitle2" fontWeight={700}>{adjustItem.productName}</Typography>
              <Typography variant="caption" color="text.secondary">{adjustItem.branchName}</Typography>
            </Box>
          )}
          {adjustError && <Alert severity="error" sx={{ mb: 2 }}>{adjustError}</Alert>}
          <TextField
            fullWidth
            label="New Quantity"
            type="number"
            value={newQty}
            onChange={(e) => setNewQty(e.target.value)}
            inputProps={{ min: 0 }}
            sx={{ mb: 2 }}
          />
          <TextField
            fullWidth
            label="Reason (optional)"
            value={adjustReason}
            onChange={(e) => setAdjustReason(e.target.value)}
            placeholder="e.g. Stocktake, Damaged goods…"
            size="small"
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setAdjustOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleAdjust} disabled={adjustMutation.isPending}>
            {adjustMutation.isPending ? <CircularProgress size={20} color="inherit" /> : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>
    </Layout>
  );
};
