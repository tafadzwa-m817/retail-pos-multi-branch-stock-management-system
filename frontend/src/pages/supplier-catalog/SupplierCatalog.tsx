import {
  Box, Card, Typography, Chip, Button, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, FormControl, InputLabel, Select, MenuItem,
  Tooltip, IconButton, Alert, CircularProgress, Switch, FormControlLabel,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import StarIcon from '@mui/icons-material/Star';
import StarBorderIcon from '@mui/icons-material/StarBorder';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { Layout } from '../../components/layout/Layout';
import { supplierProductsApi, type SupplierProduct } from '../../api/supplierProducts';
import { productsApi } from '../../api/products';
import { useToast } from '../../context/ToastContext';
import apiClient from '../../api/client';
import type { ApiResponse, SupplierResponse } from '../../types';

const fmt = (n: number) => `$${Number(n).toFixed(2)}`;

export const SupplierCatalog = () => {
  const qc = useQueryClient();
  const { showSuccess, showError } = useToast();
  const [selectedSupplierId, setSelectedSupplierId] = useState<number | ''>('');
  const [addOpen, setAddOpen] = useState(false);

  const { data: suppliers = [] } = useQuery({
    queryKey: ['suppliers'],
    queryFn: () => apiClient.get<ApiResponse<SupplierResponse[]>>('/suppliers').then((r) => r.data.data),
  });

  const { data: products = [] } = useQuery({ queryKey: ['products'], queryFn: productsApi.getAll });

  const { data: catalog = [], isLoading } = useQuery({
    queryKey: ['supplier-catalog', selectedSupplierId],
    queryFn: () => selectedSupplierId ? supplierProductsApi.getCatalog(selectedSupplierId) : Promise.resolve([]),
    enabled: !!selectedSupplierId,
  });

  const { register, handleSubmit, reset, watch } = useForm<{ productId: number | ''; unitCost: number | ''; supplierSku: string; preferred: boolean }>({
    defaultValues: { productId: '', unitCost: '', preferred: false },
  });

  const upsertMutation = useMutation({
    mutationFn: (d: { productId: number | ''; unitCost: number | ''; supplierSku: string; preferred: boolean }) =>
      supplierProductsApi.upsert({ supplierId: Number(selectedSupplierId), productId: Number(d.productId), unitCost: Number(d.unitCost), supplierSku: d.supplierSku, preferred: d.preferred }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['supplier-catalog'] }); setAddOpen(false); reset(); showSuccess('Catalog updated'); },
    onError: (e: unknown) => showError((e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Error'),
  });

  const deleteMutation = useMutation({
    mutationFn: supplierProductsApi.delete,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['supplier-catalog'] }); showSuccess('Removed from catalog'); },
    onError: () => showError('Failed to remove'),
  });

  const cols: GridColDef<SupplierProduct>[] = [
    { field: 'product', headerName: 'Product', flex: 1.5, minWidth: 180, renderCell: ({ value }) => (value as { name: string }).name },
    { field: 'supplierSku', headerName: 'Supplier SKU', flex: 1, minWidth: 130, renderCell: ({ value }) => value ?? '—' },
    { field: 'unitCost', headerName: 'Unit Cost', width: 120, renderCell: ({ value }) => <Typography fontWeight={700} color="success.main">{fmt(Number(value))}</Typography> },
    {
      field: 'preferred', headerName: 'Preferred', width: 100,
      renderCell: ({ value }) => value ? <StarIcon color="warning" fontSize="small" /> : <StarBorderIcon color="disabled" fontSize="small" />,
    },
    { field: 'lastUpdated', headerName: 'Last Updated', flex: 1, minWidth: 150, renderCell: ({ value }) => value ? new Date(value).toLocaleDateString() : '—' },
    {
      field: 'actions', headerName: '', width: 80, sortable: false,
      renderCell: ({ row }) => (
        <Tooltip title="Remove from catalog">
          <IconButton size="small" color="error" onClick={() => deleteMutation.mutate(row.id)}><DeleteIcon fontSize="small" /></IconButton>
        </Tooltip>
      ),
    },
  ];

  const selectedSupplier = suppliers.find((s) => s.id === selectedSupplierId);

  return (
    <Layout title="Supplier Price Catalog">
      <Box display="flex" gap={2} mb={2} alignItems="center" flexWrap="wrap">
        <FormControl size="small" sx={{ minWidth: 260 }}>
          <InputLabel>Select Supplier</InputLabel>
          <Select value={selectedSupplierId} label="Select Supplier" onChange={(e) => setSelectedSupplierId(e.target.value as number | '')}>
            {suppliers.map((s) => <MenuItem key={s.id} value={s.id}>{s.name}</MenuItem>)}
          </Select>
        </FormControl>
        {selectedSupplier && <Chip label={`${catalog.length} products in catalog`} variant="outlined" />}
        <Box flex={1} />
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setAddOpen(true)} disabled={!selectedSupplierId}>
          Add Product
        </Button>
      </Box>

      {!selectedSupplierId ? (
        <Box textAlign="center" py={8} color="text.secondary">
          <Typography variant="h6">Select a supplier to view and manage their price catalog</Typography>
          <Typography variant="body2" mt={1}>Catalog prices auto-fill unit costs when creating purchase orders</Typography>
        </Box>
      ) : (
        <Card>
          <DataGrid
            rows={catalog} columns={cols} loading={isLoading} autoHeight
            pageSizeOptions={[25]} initialState={{ pagination: { paginationModel: { pageSize: 25 } } }}
            disableRowSelectionOnClick sx={{ border: 0 }}
          />
        </Card>
      )}

      <Dialog open={addOpen} onClose={() => { setAddOpen(false); reset(); }} maxWidth="xs" fullWidth>
        <DialogTitle fontWeight={700}>Add Product to {selectedSupplier?.name ?? ''} Catalog</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} mt={1}>
            <FormControl fullWidth size="small">
              <InputLabel>Product *</InputLabel>
              <Select value={watch('productId')} label="Product *" onChange={(e) => reset({ ...watch(), productId: e.target.value as number })}>
                {products.map((p) => <MenuItem key={p.id} value={p.id}>{p.name}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField fullWidth size="small" label="Unit Cost ($) *" type="number" inputProps={{ min: 0.01, step: 0.01 }} {...register('unitCost', { required: true })} />
            <TextField fullWidth size="small" label="Supplier SKU (optional)" {...register('supplierSku')} />
            <FormControlLabel control={<Switch {...register('preferred')} />} label="Mark as preferred supplier for this product" />
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => { setAddOpen(false); reset(); }}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmit((d) => upsertMutation.mutate(d))} disabled={upsertMutation.isPending}>
            {upsertMutation.isPending ? <CircularProgress size={20} color="inherit" /> : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>
    </Layout>
  );
};
