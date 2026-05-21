import {
  Box, Card, Tabs, Tab, Typography, Button, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, Alert, CircularProgress, IconButton, Tooltip,
  Chip, FormControl, InputLabel, Select, MenuItem, Switch, FormControlLabel,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import PersonIcon from '@mui/icons-material/Person';
import StoreIcon from '@mui/icons-material/Store';
import CategoryIcon from '@mui/icons-material/Category';
import ReceiptIcon from '@mui/icons-material/Receipt';
import TuneIcon from '@mui/icons-material/Tune';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm, Controller } from 'react-hook-form';
import { Layout } from '../../components/layout/Layout';
import { branchesApi } from '../../api/branches';
import { categoriesApi } from '../../api/categories';
import { taxRatesApi, type TaxRate } from '../../api/taxRates';
import { storeSettingsApi, type StoreSettingsRequest } from '../../api/storeSettings';
import { useToast } from '../../context/ToastContext';
import apiClient from '../../api/client';
import type { ApiResponse, BranchResponse, CategoryResponse, UserResponse } from '../../types';

export const Settings = () => {
  const [tab, setTab] = useState(0);

  return (
    <Layout title="Settings">
      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 3, borderBottom: '1px solid', borderColor: 'divider' }}>
        <Tab icon={<PersonIcon />} iconPosition="start" label="Users" />
        <Tab icon={<StoreIcon />} iconPosition="start" label="Branches" />
        <Tab icon={<CategoryIcon />} iconPosition="start" label="Categories" />
        <Tab icon={<ReceiptIcon />} iconPosition="start" label="Tax Rates" />
        <Tab icon={<TuneIcon />} iconPosition="start" label="Store Profile" />
      </Tabs>

      {tab === 0 && <UsersTab />}
      {tab === 1 && <BranchesTab />}
      {tab === 2 && <CategoriesTab />}
      {tab === 3 && <TaxRatesTab />}
      {tab === 4 && <StoreProfileTab />}
    </Layout>
  );
};

// ── Users Tab ─────────────────────────────────────────────────────────────────
const UsersTab = () => {
  const { data: users = [], isLoading } = useQuery({
    queryKey: ['users'],
    queryFn: () => apiClient.get<ApiResponse<UserResponse[]>>('/users').then((r) => r.data.data),
  });

  const cols: GridColDef<UserResponse>[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'firstName', headerName: 'First Name', flex: 1 },
    { field: 'lastName', headerName: 'Last Name', flex: 1 },
    { field: 'email', headerName: 'Email', flex: 1.5 },
    { field: 'role', headerName: 'Role', width: 140, renderCell: ({ value }) => <Chip label={value?.replace('_', ' ')} size="small" color="primary" variant="outlined" /> },
    { field: 'branchName', headerName: 'Branch', flex: 1, renderCell: ({ value }) => value ?? '—' },
    { field: 'active', headerName: 'Status', width: 90, renderCell: ({ value }) => <Chip label={value ? 'Active' : 'Inactive'} color={value ? 'success' : 'default'} size="small" /> },
  ];

  return (
    <Card>
      <DataGrid rows={users} columns={cols} loading={isLoading} autoHeight pageSizeOptions={[25]} initialState={{ pagination: { paginationModel: { pageSize: 25 } } }} disableRowSelectionOnClick sx={{ border: 0 }} />
    </Card>
  );
};

// ── Branches Tab ──────────────────────────────────────────────────────────────
const BranchesTab = () => {
  const qc = useQueryClient();
  const [open, setOpen] = useState(false);
  const [edit, setEdit] = useState<BranchResponse | null>(null);
  const [err, setErr] = useState('');
  const { register, handleSubmit, reset, setValue, formState: { errors } } = useForm<{ name: string; address: string; phone: string; email: string; active: boolean }>();
  const { data: branches = [], isLoading } = useQuery({ queryKey: ['branches-all'], queryFn: branchesApi.getAll });

  const saveMutation = useMutation({
    mutationFn: (data: { name: string; address: string; phone: string; email: string; active: boolean } & { id?: number }) =>
      data.id
        ? apiClient.put<ApiResponse<BranchResponse>>(`/branches/${data.id}`, data).then((r) => r.data.data)
        : apiClient.post<ApiResponse<BranchResponse>>('/branches', data).then((r) => r.data.data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['branches-all'] }); qc.invalidateQueries({ queryKey: ['branches'] }); close(); },
    onError: (e: unknown) => setErr((e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Error'),
  });

  const close = () => { setOpen(false); setEdit(null); reset(); setErr(''); };
  const openEdit = (b: BranchResponse) => { setEdit(b); setValue('name', b.name); setValue('address', b.address ?? ''); setValue('phone', b.phone ?? ''); setValue('email', b.email ?? ''); setValue('active', b.active); setOpen(true); };

  const cols: GridColDef<BranchResponse>[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'name', headerName: 'Name', flex: 1.5 },
    { field: 'address', headerName: 'Address', flex: 2 },
    { field: 'phone', headerName: 'Phone', flex: 1 },
    { field: 'active', headerName: 'Status', width: 90, renderCell: ({ value }) => <Chip label={value ? 'Active' : 'Inactive'} color={value ? 'success' : 'default'} size="small" /> },
    { field: 'actions', headerName: '', width: 80, sortable: false, renderCell: ({ row }) => <Tooltip title="Edit"><IconButton size="small" onClick={() => openEdit(row)}><EditIcon fontSize="small" /></IconButton></Tooltip> },
  ];

  return (
    <>
      <Box display="flex" justifyContent="flex-end" mb={2}>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => { reset({ active: true }); setOpen(true); }}>New Branch</Button>
      </Box>
      <Card>
        <DataGrid rows={branches} columns={cols} loading={isLoading} autoHeight pageSizeOptions={[20]} initialState={{ pagination: { paginationModel: { pageSize: 20 } } }} disableRowSelectionOnClick sx={{ border: 0 }} />
      </Card>
      <Dialog open={open} onClose={close} maxWidth="xs" fullWidth>
        <DialogTitle fontWeight={700}>{edit ? 'Edit Branch' : 'New Branch'}</DialogTitle>
        <DialogContent>
          {err && <Alert severity="error" sx={{ mb: 2 }}>{err}</Alert>}
          <Box display="flex" flexDirection="column" gap={2} mt={1}>
            <TextField fullWidth size="small" label="Branch Name *" {...register('name', { required: 'Required' })} error={!!errors.name} helperText={errors.name?.message} />
            <TextField fullWidth size="small" label="Address" {...register('address')} />
            <Box display="flex" gap={2}><TextField fullWidth size="small" label="Phone" {...register('phone')} /><TextField fullWidth size="small" label="Email" {...register('email')} /></Box>
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={close}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmit((d) => saveMutation.mutate({ ...d, id: edit?.id }))} disabled={saveMutation.isPending}>
            {saveMutation.isPending ? <CircularProgress size={20} color="inherit" /> : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

// ── Categories Tab ────────────────────────────────────────────────────────────
const CategoriesTab = () => {
  const qc = useQueryClient();
  const [open, setOpen] = useState(false);
  const [err, setErr] = useState('');
  const { register, handleSubmit, reset } = useForm<{ name: string; description: string }>();
  const { data: categories = [], isLoading } = useQuery({ queryKey: ['categories'], queryFn: categoriesApi.getAll });
  const createMutation = useMutation({
    mutationFn: (data: { name: string; description: string }) => categoriesApi.create(data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['categories'] }); setOpen(false); reset(); },
    onError: (e: unknown) => setErr((e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Error'),
  });

  const cols: GridColDef<CategoryResponse>[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'name', headerName: 'Category', flex: 1 },
    { field: 'description', headerName: 'Description', flex: 2 },
  ];

  return (
    <>
      <Box display="flex" justifyContent="flex-end" mb={2}>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpen(true)}>New Category</Button>
      </Box>
      <Card>
        <DataGrid rows={categories} columns={cols} loading={isLoading} autoHeight pageSizeOptions={[20]} initialState={{ pagination: { paginationModel: { pageSize: 20 } } }} disableRowSelectionOnClick sx={{ border: 0 }} />
      </Card>
      <Dialog open={open} onClose={() => { setOpen(false); reset(); }} maxWidth="xs" fullWidth>
        <DialogTitle fontWeight={700}>New Category</DialogTitle>
        <DialogContent>
          {err && <Alert severity="error" sx={{ mb: 2 }}>{err}</Alert>}
          <Box display="flex" flexDirection="column" gap={2} mt={1}>
            <TextField fullWidth size="small" label="Name *" {...register('name', { required: 'Required' })} />
            <TextField fullWidth size="small" label="Description" {...register('description')} />
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmit((d) => createMutation.mutate(d))} disabled={createMutation.isPending}>Save</Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

// ── Tax Rates Tab ─────────────────────────────────────────────────────────────
const TaxRatesTab = () => {
  const qc = useQueryClient();
  const [open, setOpen] = useState(false);
  const { register, handleSubmit, reset } = useForm<{ name: string; rate: number }>();
  const { data: rates = [], isLoading } = useQuery({ queryKey: ['tax-rates'], queryFn: taxRatesApi.getAll });

  const createMutation = useMutation({
    mutationFn: (d: { name: string; rate: number }) => taxRatesApi.create(d.name, d.rate),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['tax-rates'] }); setOpen(false); reset(); },
  });

  const toggleMutation = useMutation({
    mutationFn: (r: TaxRate) => taxRatesApi.update(r.id, r.name, r.rate, !r.active),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['tax-rates'] }),
  });

  const cols: GridColDef<TaxRate>[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'name', headerName: 'Name', flex: 1 },
    { field: 'rate', headerName: 'Rate', width: 100, renderCell: ({ value }) => `${value}%` },
    { field: 'active', headerName: 'Active', width: 100, renderCell: ({ row }) => <Switch checked={row.active} onChange={() => toggleMutation.mutate(row)} size="small" /> },
  ];

  return (
    <>
      <Box display="flex" justifyContent="flex-end" mb={2}>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpen(true)}>New Tax Rate</Button>
      </Box>
      <Card>
        <DataGrid rows={rates} columns={cols} loading={isLoading} autoHeight pageSizeOptions={[20]} initialState={{ pagination: { paginationModel: { pageSize: 20 } } }} disableRowSelectionOnClick sx={{ border: 0 }} />
      </Card>
      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle fontWeight={700}>New Tax Rate</DialogTitle>
        <DialogContent>
          <Box display="flex" gap={2} mt={1}>
            <TextField fullWidth size="small" label="Name (e.g. VAT)" {...register('name', { required: true })} />
            <TextField fullWidth size="small" label="Rate (%)" type="number" inputProps={{ step: 0.5, min: 0 }} {...register('rate', { required: true })} />
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmit((d) => createMutation.mutate(d))}>Save</Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

// ── Store Profile Tab ─────────────────────────────────────────────────────────
const StoreProfileTab = () => {
  const qc = useQueryClient();
  const { showSuccess, showError } = useToast();
  const { data: settings, isLoading } = useQuery({ queryKey: ['store-settings'], queryFn: storeSettingsApi.get });

  const { register, handleSubmit, formState: { isDirty } } = useForm<StoreSettingsRequest>();

  const updateMutation = useMutation({
    mutationFn: storeSettingsApi.update,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['store-settings'] }); showSuccess('Store settings updated'); },
    onError: () => showError('Failed to update settings'),
  });

  if (isLoading) return <CircularProgress />;

  return (
    <Card sx={{ maxWidth: 600 }}>
      <Box p={3}>
        <Typography variant="h6" fontWeight={700} mb={3} display="flex" alignItems="center" gap={1}>
          <TuneIcon color="primary" fontSize="small" />
          Store Profile & Receipt Customisation
        </Typography>
        <Box component="form" display="flex" flexDirection="column" gap={2}>
          <TextField fullWidth size="small" label="Store Name" defaultValue={settings?.storeName} {...register('storeName')} />
          <Box display="flex" gap={2}>
            <TextField fullWidth size="small" label="Phone" defaultValue={settings?.storePhone} {...register('storePhone')} />
            <FormControl fullWidth size="small">
              <InputLabel>Currency</InputLabel>
              <Select defaultValue={settings?.currency ?? 'USD'} label="Currency" {...register('currency')}>
                <MenuItem value="USD">USD ($)</MenuItem>
                <MenuItem value="ZWL">ZWL (Z$)</MenuItem>
                <MenuItem value="ZAR">ZAR (R)</MenuItem>
              </Select>
            </FormControl>
          </Box>
          <TextField fullWidth size="small" label="Address" defaultValue={settings?.storeAddress} {...register('storeAddress')} />
          <TextField fullWidth size="small" label="Logo URL" placeholder="https://..." defaultValue={settings?.logoUrl ?? ''} {...register('logoUrl')} helperText="Shown on printed receipts" />
          <TextField fullWidth size="small" label="Receipt Footer Text" multiline rows={2} defaultValue={settings?.receiptFooterText} {...register('receiptFooterText')} helperText="Appears at the bottom of every receipt" />
          <Box display="flex" justifyContent="flex-end">
            <Button variant="contained" onClick={handleSubmit((d) => updateMutation.mutate(d))} disabled={updateMutation.isPending}>
              {updateMutation.isPending ? <CircularProgress size={20} color="inherit" /> : 'Save Settings'}
            </Button>
          </Box>
        </Box>
      </Box>
    </Card>
  );
};
