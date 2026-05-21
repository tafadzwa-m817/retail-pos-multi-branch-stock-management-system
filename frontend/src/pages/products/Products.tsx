import {
  Box, Card, Button, Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Typography, Chip, Alert, CircularProgress, IconButton,
  Tooltip, FormControl, InputLabel, Select, MenuItem, InputAdornment,
  Switch, FormControlLabel,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import SearchIcon from '@mui/icons-material/Search';
import UploadIcon from '@mui/icons-material/Upload';
import DownloadIcon from '@mui/icons-material/Download';
import LabelIcon from '@mui/icons-material/Label';
import { useRef, useState, useMemo } from 'react';
import { PriceLabels } from '../../components/products/PriceLabels';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm, Controller } from 'react-hook-form';
import { Layout } from '../../components/layout/Layout';
import { productsApi } from '../../api/products';
import { categoriesApi } from '../../api/categories';
import apiClient from '../../api/client';
import type { ApiResponse, CategoryResponse, ProductResponse } from '../../types';
import { useToast } from '../../context/ToastContext';

const PRODUCT_CSV_TEMPLATE = `name,sku,barcode,categoryName,costPrice,sellingPrice,reorderLevel
Coca-Cola 500ml,COKE-500ML,6001234567895,Food & Beverages,0.60,1.00,100
iPhone Case 15,IPH-CASE-15,,Electronics,8.00,15.00,20
Men Polo Shirt M,POL-M-MED,,Clothing,12.00,25.00,15`;

const downloadTemplate = () => {
  const blob = new Blob([PRODUCT_CSV_TEMPLATE], { type: 'text/csv' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = 'product-import-template.csv'; a.click();
  URL.revokeObjectURL(url);
};

interface ProductForm {
  name: string;
  sku: string;
  barcode: string;
  description: string;
  categoryId: number | '';
  costPrice: number | '';
  sellingPrice: number | '';
  reorderLevel: number;
  active: boolean;
}

export const Products = () => {
  const queryClient = useQueryClient();
  const { showSuccess, showError } = useToast();
  const [search, setSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState<number | ''>('');
  const [formOpen, setFormOpen] = useState(false);
  const [editProduct, setEditProduct] = useState<ProductResponse | null>(null);
  const [formError, setFormError] = useState('');
  const [importOpen, setImportOpen] = useState(false);
  const [importResult, setImportResult] = useState<{ created: number; updated: number; skipped: number; errors: string[] } | null>(null);
  const [importing, setImporting] = useState(false);
  const [labelsOpen, setLabelsOpen] = useState(false);
  const [selectedRows, setSelectedRows] = useState<number[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const { data: categories = [] } = useQuery({ queryKey: ['categories'], queryFn: categoriesApi.getAll });

  const { data: products = [], isLoading } = useQuery({
    queryKey: ['products-all', search],
    queryFn: () => search.length >= 2 ? productsApi.search(search) : productsApi.getAllAdmin(),
  });

  const { register, handleSubmit, reset, control, setValue, formState: { errors } } = useForm<ProductForm>({
    defaultValues: { reorderLevel: 10, active: true, categoryId: '', costPrice: '', sellingPrice: '' },
  });

  const saveMutation = useMutation({
    mutationFn: (data: ProductForm & { id?: number }) => {
      const payload = {
        name: data.name, sku: data.sku, barcode: data.barcode,
        description: data.description, categoryId: Number(data.categoryId),
        costPrice: Number(data.costPrice), sellingPrice: Number(data.sellingPrice),
        reorderLevel: data.reorderLevel, active: data.active,
      };
      return data.id
        ? apiClient.put<ApiResponse<ProductResponse>>(`/products/${data.id}`, payload).then((r) => r.data.data)
        : apiClient.post<ApiResponse<ProductResponse>>('/products', payload).then((r) => r.data.data);
    },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['products-all'] }); queryClient.invalidateQueries({ queryKey: ['products'] }); closeForm(); },
    onError: (err: unknown) => setFormError((err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Error'),
  });

  const closeForm = () => { setFormOpen(false); setEditProduct(null); reset(); setFormError(''); };

  const handleFileImport = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setImporting(true);
    setImportResult(null);
    try {
      const formData = new FormData();
      formData.append('file', file);
      const res = await apiClient.post<ApiResponse<{ created: number; updated: number; skipped: number; errors: string[] }>>('/products/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
      setImportResult(res.data.data);
      queryClient.invalidateQueries({ queryKey: ['products-all'] });
      queryClient.invalidateQueries({ queryKey: ['products'] });
      showSuccess(`Import done: ${res.data.data.created} created, ${res.data.data.updated} updated`);
    } catch (err: unknown) {
      showError((err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Import failed');
    } finally {
      setImporting(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const openCreate = () => {
    reset({ reorderLevel: 10, active: true, categoryId: '', costPrice: '', sellingPrice: '' });
    setEditProduct(null);
    setFormOpen(true);
  };

  const openEdit = (p: ProductResponse) => {
    setEditProduct(p);
    setValue('name', p.name); setValue('sku', p.sku ?? ''); setValue('barcode', p.barcode ?? '');
    setValue('description', p.description ?? ''); setValue('categoryId', p.categoryId);
    setValue('costPrice', p.costPrice); setValue('sellingPrice', p.sellingPrice);
    setValue('reorderLevel', p.reorderLevel); setValue('active', p.active);
    setFormOpen(true);
  };

  const onSubmit = (data: ProductForm) => {
    saveMutation.mutate({ ...data, id: editProduct?.id });
  };

  const filtered = categoryFilter ? products.filter((p) => p.categoryId === categoryFilter) : products;

  const columns: GridColDef<ProductResponse>[] = [
    { field: 'name', headerName: 'Product', flex: 1.5, minWidth: 180 },
    { field: 'sku', headerName: 'SKU', flex: 0.8, minWidth: 100, renderCell: ({ value }) => value ?? '—' },
    { field: 'barcode', headerName: 'Barcode', flex: 0.8, minWidth: 120, renderCell: ({ value }) => value ?? '—' },
    { field: 'categoryName', headerName: 'Category', flex: 1, minWidth: 130, renderCell: ({ value }) => <Chip label={value} size="small" variant="outlined" /> },
    { field: 'sellingPrice', headerName: 'Price', width: 100, renderCell: ({ value }) => <Typography fontWeight={700}>${Number(value).toFixed(2)}</Typography> },
    { field: 'costPrice', headerName: 'Cost', width: 90, renderCell: ({ value }) => `$${Number(value).toFixed(2)}` },
    { field: 'reorderLevel', headerName: 'Reorder', width: 90 },
    {
      field: 'active', headerName: 'Status', width: 100,
      renderCell: ({ value }) => <Chip label={value ? 'Active' : 'Inactive'} color={value ? 'success' : 'default'} size="small" />,
    },
    {
      field: 'actions', headerName: '', width: 80, sortable: false,
      renderCell: ({ row }) => (
        <Tooltip title="Edit"><IconButton size="small" onClick={() => openEdit(row)}><EditIcon fontSize="small" /></IconButton></Tooltip>
      ),
    },
  ];

  return (
    <Layout title="Products">
      <Box display="flex" gap={2} mb={2} flexWrap="wrap" alignItems="center">
        <TextField
          size="small" placeholder="Search name, SKU, barcode…" value={search}
          onChange={(e) => setSearch(e.target.value)} sx={{ minWidth: 260 }}
          InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" color="action" /></InputAdornment> }}
        />
        <FormControl size="small" sx={{ minWidth: 160 }}>
          <InputLabel>Category</InputLabel>
          <Select value={categoryFilter} label="Category" onChange={(e) => setCategoryFilter(e.target.value as number | '')}>
            <MenuItem value="">All Categories</MenuItem>
            {categories.map((c) => <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>)}
          </Select>
        </FormControl>
        <Box flex={1} />
        <Typography variant="body2" color="text.secondary">{filtered.length} products</Typography>
        <Button variant="outlined" startIcon={<DownloadIcon />} onClick={downloadTemplate} size="small">
          CSV Template
        </Button>
        <Button variant="outlined" startIcon={<UploadIcon />} onClick={() => fileInputRef.current?.click()} disabled={importing} size="small">
          {importing ? <CircularProgress size={16} /> : 'Import CSV'}
        </Button>
        <input ref={fileInputRef} type="file" accept=".csv" style={{ display: 'none' }} onChange={handleFileImport} />
        <Button
          variant="outlined"
          startIcon={<LabelIcon />}
          onClick={() => setLabelsOpen(true)}
          disabled={filtered.length === 0}
        >
          Print Labels
        </Button>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>New Product</Button>
      </Box>

      {importResult && (
        <Alert severity={importResult.skipped > 0 ? 'warning' : 'success'} onClose={() => setImportResult(null)} sx={{ mb: 2 }}>
          Import complete — {importResult.created} created, {importResult.updated} updated, {importResult.skipped} skipped.
          {importResult.errors.length > 0 && <Box mt={0.5}><strong>Errors:</strong> {importResult.errors.slice(0, 3).join(' | ')}{importResult.errors.length > 3 && ` +${importResult.errors.length - 3} more`}</Box>}
        </Alert>
      )}

      <Card>
        <DataGrid rows={filtered} columns={columns} loading={isLoading} autoHeight pageSizeOptions={[25, 50, 100]} initialState={{ pagination: { paginationModel: { pageSize: 25 } } }} disableRowSelectionOnClick sx={{ border: 0 }} />
      </Card>

      <Dialog open={formOpen} onClose={closeForm} maxWidth="sm" fullWidth>
        <DialogTitle fontWeight={700}>{editProduct ? 'Edit Product' : 'New Product'}</DialogTitle>
        <DialogContent>
          {formError && <Alert severity="error" sx={{ mb: 2 }}>{formError}</Alert>}
          <Box display="flex" flexDirection="column" gap={2} mt={1}>
            <TextField fullWidth size="small" label="Product Name *" {...register('name', { required: 'Required' })} error={!!errors.name} helperText={errors.name?.message} />
            <Box display="flex" gap={2}>
              <TextField fullWidth size="small" label="SKU" {...register('sku')} />
              <TextField fullWidth size="small" label="Barcode" {...register('barcode')} />
            </Box>
            <FormControl fullWidth size="small">
              <InputLabel>Category *</InputLabel>
              <Controller name="categoryId" control={control} rules={{ required: 'Required' }}
                render={({ field }) => (
                  <Select {...field} label="Category *" error={!!errors.categoryId}>
                    {categories.map((c) => <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>)}
                  </Select>
                )} />
            </FormControl>
            <Box display="flex" gap={2}>
              <TextField fullWidth size="small" label="Cost Price ($) *" type="number" inputProps={{ step: 0.01, min: 0 }} {...register('costPrice', { required: 'Required', min: { value: 0, message: 'Must be ≥ 0' } })} error={!!errors.costPrice} helperText={errors.costPrice?.message} />
              <TextField fullWidth size="small" label="Selling Price ($) *" type="number" inputProps={{ step: 0.01, min: 0 }} {...register('sellingPrice', { required: 'Required', min: { value: 0.01, message: 'Must be > 0' } })} error={!!errors.sellingPrice} helperText={errors.sellingPrice?.message} />
            </Box>
            <TextField fullWidth size="small" label="Reorder Level" type="number" inputProps={{ min: 0 }} {...register('reorderLevel')} />
            <TextField fullWidth size="small" label="Description" multiline rows={2} {...register('description')} />
            <Controller name="active" control={control}
              render={({ field }) => (
                <FormControlLabel control={<Switch checked={field.value} onChange={field.onChange} />} label="Active" />
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

      <PriceLabels
        products={filtered}
        open={labelsOpen}
        onClose={() => setLabelsOpen(false)}
      />
    </Layout>
  );
};
