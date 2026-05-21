import {
  Box, Card, Button, Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Typography, Chip, Alert, CircularProgress, IconButton,
  Tooltip, InputAdornment,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import HistoryIcon from '@mui/icons-material/History';
import SearchIcon from '@mui/icons-material/Search';
import StarIcon from '@mui/icons-material/Star';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { Layout } from '../../components/layout/Layout';
import { customersApi } from '../../api/customers';
import { salesApi } from '../../api/sales';
import { Receipt } from '../../components/pos/Receipt';
import type { CustomerResponse, SaleResponse } from '../../types';

interface CustomerForm {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  address: string;
}

export const Customers = () => {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [editCustomer, setEditCustomer] = useState<CustomerResponse | null>(null);
  const [historyCustomer, setHistoryCustomer] = useState<CustomerResponse | null>(null);
  const [formError, setFormError] = useState('');
  const [viewSale, setViewSale] = useState<SaleResponse | null>(null);

  const { data: customers = [], isLoading } = useQuery({
    queryKey: ['customers', search],
    queryFn: () => search.length >= 2 ? customersApi.search(search) : customersApi.getAll(),
  });

  const { data: purchaseHistory = [], isLoading: historyLoading } = useQuery({
    queryKey: ['customer-history', historyCustomer?.id],
    queryFn: () => historyCustomer ? salesApi.getAll(0, 100).then((p) => p.content.filter((s) => s.customerId === historyCustomer.id)) : Promise.resolve([]),
    enabled: !!historyCustomer,
  });

  const { register, handleSubmit, reset, setValue, formState: { errors } } = useForm<CustomerForm>();

  const createMutation = useMutation({
    mutationFn: (data: CustomerForm) => customersApi.create(data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['customers'] }); closeForm(); },
    onError: (err: unknown) => setFormError((err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Error'),
  });

  const closeForm = () => { setFormOpen(false); setEditCustomer(null); reset(); setFormError(''); };

  const openCreate = () => { reset(); setEditCustomer(null); setFormOpen(true); };
  const openEdit = (c: CustomerResponse) => {
    setEditCustomer(c);
    setValue('firstName', c.firstName);
    setValue('lastName', c.lastName);
    setValue('email', c.email ?? '');
    setValue('phone', c.phone ?? '');
    setValue('address', c.address ?? '');
    setFormOpen(true);
  };

  const onSubmit = (data: CustomerForm) => {
    createMutation.mutate(data);
  };

  const columns: GridColDef<CustomerResponse>[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    {
      field: 'firstName', headerName: 'Name', flex: 1.5, minWidth: 160,
      renderCell: ({ row }) => `${row.firstName} ${row.lastName}`,
    },
    { field: 'email', headerName: 'Email', flex: 1.5, minWidth: 180 },
    { field: 'phone', headerName: 'Phone', flex: 1, minWidth: 130 },
    {
      field: 'loyaltyPoints', headerName: 'Loyalty', width: 110,
      renderCell: ({ value }) => (
        <Chip icon={<StarIcon />} label={value} size="small" color={value > 0 ? 'warning' : 'default'} />
      ),
    },
    {
      field: 'createdAt', headerName: 'Member Since', flex: 1, minWidth: 130,
      renderCell: ({ value }) => new Date(value).toLocaleDateString(),
    },
    {
      field: 'actions', headerName: '', width: 100, sortable: false,
      renderCell: ({ row }) => (
        <Box display="flex" gap={0.5}>
          <Tooltip title="Edit"><IconButton size="small" onClick={() => openEdit(row)}><EditIcon fontSize="small" /></IconButton></Tooltip>
          <Tooltip title="Purchase history"><IconButton size="small" onClick={() => setHistoryCustomer(row)}><HistoryIcon fontSize="small" /></IconButton></Tooltip>
        </Box>
      ),
    },
  ];

  return (
    <Layout title="Customers">
      <Box display="flex" gap={2} mb={2} alignItems="center">
        <TextField
          size="small"
          placeholder="Search by name, email or phone…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          sx={{ minWidth: 280 }}
          InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" color="action" /></InputAdornment> }}
        />
        <Box flex={1} />
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>New Customer</Button>
      </Box>

      <Card>
        <DataGrid rows={customers} columns={columns} loading={isLoading} autoHeight pageSizeOptions={[25, 50]} initialState={{ pagination: { paginationModel: { pageSize: 25 } } }} disableRowSelectionOnClick sx={{ border: 0 }} />
      </Card>

      {/* Create/Edit Dialog */}
      <Dialog open={formOpen} onClose={closeForm} maxWidth="xs" fullWidth>
        <DialogTitle fontWeight={700}>{editCustomer ? 'Edit Customer' : 'New Customer'}</DialogTitle>
        <DialogContent>
          {formError && <Alert severity="error" sx={{ mb: 2 }}>{formError}</Alert>}
          <Box component="form" display="flex" flexDirection="column" gap={2} mt={1}>
            <Box display="flex" gap={2}>
              <TextField fullWidth size="small" label="First Name" {...register('firstName', { required: 'Required' })} error={!!errors.firstName} helperText={errors.firstName?.message} />
              <TextField fullWidth size="small" label="Last Name" {...register('lastName', { required: 'Required' })} error={!!errors.lastName} helperText={errors.lastName?.message} />
            </Box>
            <TextField fullWidth size="small" label="Email" type="email" {...register('email')} />
            <TextField fullWidth size="small" label="Phone" {...register('phone')} />
            <TextField fullWidth size="small" label="Address" {...register('address')} />
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={closeForm}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmit(onSubmit)} disabled={createMutation.isPending}>
            {createMutation.isPending ? <CircularProgress size={20} color="inherit" /> : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Purchase History Dialog */}
      <Dialog open={!!historyCustomer} onClose={() => setHistoryCustomer(null)} maxWidth="md" fullWidth>
        <DialogTitle fontWeight={700}>
          Purchase History — {historyCustomer?.firstName} {historyCustomer?.lastName}
          <Chip icon={<StarIcon />} label={`${historyCustomer?.loyaltyPoints ?? 0} pts`} size="small" color="warning" sx={{ ml: 1 }} />
        </DialogTitle>
        <DialogContent>
          {historyLoading ? <CircularProgress /> : (
            purchaseHistory.length === 0 ? (
              <Typography color="text.secondary" textAlign="center" py={4}>No purchases yet</Typography>
            ) : (
              purchaseHistory.map((sale) => (
                <Box key={sale.id} display="flex" justifyContent="space-between" alignItems="center" py={1} borderBottom="1px solid #F0F4F8">
                  <Box>
                    <Typography variant="body2" fontWeight={700}>Sale #{sale.id} · {new Date(sale.createdAt).toLocaleDateString()}</Typography>
                    <Typography variant="caption" color="text.secondary">{sale.items.length} items · {sale.paymentMethod}</Typography>
                  </Box>
                  <Box display="flex" alignItems="center" gap={1}>
                    <Typography fontWeight={700} color="primary.main">${sale.totalAmount.toFixed(2)}</Typography>
                    <Chip label={sale.status} size="small" color={sale.status === 'COMPLETED' ? 'success' : 'error'} />
                    <Button size="small" onClick={() => setViewSale(sale)}>Receipt</Button>
                  </Box>
                </Box>
              ))
            )
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setHistoryCustomer(null)}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* Receipt Dialog */}
      <Dialog open={!!viewSale} onClose={() => setViewSale(null)} maxWidth="xs" fullWidth>
        <DialogTitle fontWeight={700}>Receipt #{viewSale?.id}</DialogTitle>
        <DialogContent>{viewSale && <Receipt sale={viewSale} />}</DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setViewSale(null)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Layout>
  );
};
