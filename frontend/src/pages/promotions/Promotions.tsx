import {
  Box, Card, Button, Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Typography, Chip, Alert, CircularProgress, IconButton, Tooltip,
  FormControl, InputLabel, Select, MenuItem, Switch, FormControlLabel, Autocomplete,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import LocalOfferIcon from '@mui/icons-material/LocalOffer';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm, Controller } from 'react-hook-form';
import { Layout } from '../../components/layout/Layout';
import { promotionsApi, type PromotionRequest } from '../../api/promotions';
import { productsApi } from '../../api/products';
import type { ProductResponse, PromotionResponse } from '../../types';

const toInputDate = (iso: string) => iso.slice(0, 16);
const toIso = (local: string) => new Date(local).toISOString();

export const Promotions = () => {
  const queryClient = useQueryClient();
  const [formOpen, setFormOpen] = useState(false);
  const [editPromo, setEditPromo] = useState<PromotionResponse | null>(null);
  const [formError, setFormError] = useState('');
  const [selectedProducts, setSelectedProducts] = useState<ProductResponse[]>([]);

  const { data: promotions = [], isLoading } = useQuery({ queryKey: ['promotions'], queryFn: promotionsApi.getAll });
  const { data: products = [] } = useQuery({ queryKey: ['products'], queryFn: productsApi.getAll });

  const { register, handleSubmit, control, watch, reset, setValue, formState: { errors } } = useForm<PromotionRequest>({
    defaultValues: { discountType: 'PERCENTAGE', applyToAll: true, active: true },
  });

  const applyToAll = watch('applyToAll');

  const saveMutation = useMutation({
    mutationFn: (data: PromotionRequest & { id?: number }) => {
      const payload = { ...data, startDate: toIso(data.startDate as unknown as string), endDate: toIso(data.endDate as unknown as string), productIds: selectedProducts.map((p) => p.id) };
      return data.id ? promotionsApi.update(data.id, payload) : promotionsApi.create(payload);
    },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['promotions'] }); closeForm(); },
    onError: (err: unknown) => setFormError((err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Error'),
  });

  const deleteMutation = useMutation({
    mutationFn: promotionsApi.delete,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['promotions'] }),
  });

  const closeForm = () => { setFormOpen(false); setEditPromo(null); reset(); setFormError(''); setSelectedProducts([]); };

  const openCreate = () => {
    reset({ discountType: 'PERCENTAGE', applyToAll: true, active: true });
    setSelectedProducts([]);
    setEditPromo(null);
    setFormOpen(true);
  };

  const openEdit = (p: PromotionResponse) => {
    setEditPromo(p);
    setValue('name', p.name); setValue('description', p.description ?? '');
    setValue('discountType', p.discountType as 'PERCENTAGE' | 'FIXED_AMOUNT');
    setValue('discountValue', p.discountValue);
    setValue('startDate', toInputDate(p.startDate) as unknown as string);
    setValue('endDate', toInputDate(p.endDate) as unknown as string);
    setValue('minimumPurchaseAmount', p.minimumPurchaseAmount ?? undefined);
    setValue('applyToAll', p.applyToAll);
    setValue('active', p.active);
    setSelectedProducts(products.filter((prod) => p.applicableProductIds.includes(prod.id)));
    setFormOpen(true);
  };

  const onSubmit = (data: PromotionRequest) => {
    saveMutation.mutate({ ...data, id: editPromo?.id });
  };

  const columns: GridColDef<PromotionResponse>[] = [
    { field: 'name', headerName: 'Promotion', flex: 1.5, minWidth: 180 },
    {
      field: 'discountType', headerName: 'Type', width: 130,
      renderCell: ({ row }) => (
        <Chip
          label={row.discountType === 'PERCENTAGE' ? `${row.discountValue}% OFF` : `$${row.discountValue} OFF`}
          color="secondary" size="small"
        />
      ),
    },
    { field: 'applyToAll', headerName: 'Scope', width: 110, renderCell: ({ value }) => <Chip label={value ? 'All Products' : 'Specific'} size="small" variant="outlined" /> },
    { field: 'startDate', headerName: 'Start', flex: 1, minWidth: 130, renderCell: ({ value }) => new Date(value).toLocaleDateString() },
    { field: 'endDate', headerName: 'End', flex: 1, minWidth: 130, renderCell: ({ value }) => new Date(value).toLocaleDateString() },
    {
      field: 'currentlyActive', headerName: 'Status', width: 120,
      renderCell: ({ row }) => (
        <Chip
          label={!row.active ? 'Disabled' : row.currentlyActive ? 'Active' : 'Scheduled'}
          color={!row.active ? 'default' : row.currentlyActive ? 'success' : 'warning'}
          size="small"
        />
      ),
    },
    {
      field: 'actions', headerName: '', width: 100, sortable: false,
      renderCell: ({ row }) => (
        <Box display="flex" gap={0.5}>
          <Tooltip title="Edit"><IconButton size="small" onClick={() => openEdit(row)}><EditIcon fontSize="small" /></IconButton></Tooltip>
          <Tooltip title="Delete"><IconButton size="small" color="error" onClick={() => deleteMutation.mutate(row.id)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
        </Box>
      ),
    },
  ];

  return (
    <Layout title="Promotions">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Box display="flex" alignItems="center" gap={1}>
          <LocalOfferIcon color="secondary" />
          <Typography variant="body2" color="text.secondary">
            Active promotions are automatically applied at point of sale
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>New Promotion</Button>
      </Box>

      <Card>
        <DataGrid rows={promotions} columns={columns} loading={isLoading} autoHeight pageSizeOptions={[20, 50]} initialState={{ pagination: { paginationModel: { pageSize: 20 } } }} disableRowSelectionOnClick sx={{ border: 0 }} />
      </Card>

      <Dialog open={formOpen} onClose={closeForm} maxWidth="sm" fullWidth>
        <DialogTitle fontWeight={700}>{editPromo ? 'Edit Promotion' : 'New Promotion'}</DialogTitle>
        <DialogContent>
          {formError && <Alert severity="error" sx={{ mb: 2 }}>{formError}</Alert>}
          <Box display="flex" flexDirection="column" gap={2} mt={1}>
            <TextField fullWidth size="small" label="Promotion Name *" {...register('name', { required: 'Required' })} error={!!errors.name} helperText={errors.name?.message} />
            <TextField fullWidth size="small" label="Description" {...register('description')} />

            <Box display="flex" gap={2}>
              <FormControl fullWidth size="small">
                <InputLabel>Discount Type</InputLabel>
                <Controller name="discountType" control={control}
                  render={({ field }) => (
                    <Select {...field} label="Discount Type">
                      <MenuItem value="PERCENTAGE">Percentage (%)</MenuItem>
                      <MenuItem value="FIXED_AMOUNT">Fixed Amount ($)</MenuItem>
                    </Select>
                  )} />
              </FormControl>
              <TextField fullWidth size="small" label={watch('discountType') === 'PERCENTAGE' ? 'Discount %' : 'Discount $'} type="number" inputProps={{ min: 0.01, step: 0.01 }} {...register('discountValue', { required: 'Required' })} error={!!errors.discountValue} />
            </Box>

            <Box display="flex" gap={2}>
              <TextField fullWidth size="small" label="Start Date *" type="datetime-local" InputLabelProps={{ shrink: true }} {...register('startDate', { required: 'Required' })} error={!!errors.startDate} />
              <TextField fullWidth size="small" label="End Date *" type="datetime-local" InputLabelProps={{ shrink: true }} {...register('endDate', { required: 'Required' })} error={!!errors.endDate} />
            </Box>

            <TextField fullWidth size="small" label="Min. Purchase Amount ($) — optional" type="number" inputProps={{ min: 0, step: 0.01 }} {...register('minimumPurchaseAmount')} />

            <Controller name="applyToAll" control={control}
              render={({ field }) => (
                <FormControlLabel control={<Switch checked={field.value} onChange={field.onChange} />} label="Apply to all products" />
              )} />

            {!applyToAll && (
              <Autocomplete
                multiple size="small"
                options={products}
                value={selectedProducts}
                onChange={(_, v) => setSelectedProducts(v)}
                getOptionLabel={(p) => p.name}
                renderInput={(params) => <TextField {...params} label="Applicable Products" />}
                renderTags={(value, getTagProps) =>
                  value.map((option, index) => <Chip label={option.name} size="small" {...getTagProps({ index })} />)
                }
              />
            )}

            <Controller name="active" control={control}
              render={({ field }) => (
                <FormControlLabel control={<Switch checked={field.value} onChange={field.onChange} color="success" />} label="Active" />
              )} />
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={closeForm}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmit(onSubmit)} disabled={saveMutation.isPending}>
            {saveMutation.isPending ? <CircularProgress size={20} color="inherit" /> : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>
    </Layout>
  );
};
