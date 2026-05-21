import {
  Box, Card, Button, Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Typography, Chip, CircularProgress, FormControl, InputLabel,
  Select, MenuItem, IconButton, Tooltip, Alert,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import DeleteSweepIcon from '@mui/icons-material/DeleteSweep';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm, Controller } from 'react-hook-form';
import { Layout } from '../../components/layout/Layout';
import { wastageApi, type WastageReason, type WastageRecord } from '../../api/wastage';
import { branchesApi } from '../../api/branches';
import { productsApi } from '../../api/products';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';

const REASONS: WastageReason[] = ['DAMAGED', 'EXPIRED', 'STOLEN', 'QUALITY_ISSUE', 'OTHER'];
const reasonColor = (r: WastageReason) =>
  ({ DAMAGED: 'error', EXPIRED: 'warning', STOLEN: 'error', QUALITY_ISSUE: 'warning', OTHER: 'default' } as const)[r];

interface WastageForm { branchId: number | ''; productId: number | ''; quantity: number; reason: WastageReason | ''; notes: string; wastedAt: string; }

export const Wastage = () => {
  const qc = useQueryClient();
  const { user } = useAuth();
  const { showSuccess, showError } = useToast();
  const [filterBranch, setFilterBranch] = useState<number | ''>(user?.branchId ?? '');
  const [createOpen, setCreateOpen] = useState(false);

  const { data: branches = [] } = useQuery({ queryKey: ['branches'], queryFn: branchesApi.getActive });
  const { data: products = [] } = useQuery({ queryKey: ['products'], queryFn: productsApi.getAll });
  const { data: records = [], isLoading } = useQuery({
    queryKey: ['wastage', filterBranch],
    queryFn: () => filterBranch ? wastageApi.getByBranch(filterBranch) : wastageApi.getAll(),
  });

  const { register, handleSubmit, control, reset, formState: { errors } } = useForm<WastageForm>({
    defaultValues: { branchId: user?.branchId ?? '', reason: '', wastedAt: new Date().toISOString().split('T')[0] },
  });

  const createMutation = useMutation({
    mutationFn: (d: WastageForm) => wastageApi.record({ branchId: Number(d.branchId), productId: Number(d.productId), quantity: d.quantity, reason: d.reason as WastageReason, notes: d.notes, wastedAt: d.wastedAt }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['wastage'] }); qc.invalidateQueries({ queryKey: ['inventory'] }); setCreateOpen(false); reset(); showSuccess('Wastage recorded — inventory updated'); },
    onError: (e: unknown) => showError((e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Error'),
  });

  const deleteMutation = useMutation({
    mutationFn: wastageApi.delete,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['wastage'] }); showSuccess('Wastage record deleted'); },
  });

  const totalItems = records.reduce((s, r) => s + r.quantity, 0);

  const cols: GridColDef<WastageRecord>[] = [
    { field: 'wastedAt', headerName: 'Date', width: 110, renderCell: ({ value }) => new Date(value).toLocaleDateString() },
    { field: 'product', headerName: 'Product', flex: 1.5, minWidth: 160, renderCell: ({ value }) => (value as { name: string }).name },
    { field: 'branch', headerName: 'Branch', flex: 1, minWidth: 130, renderCell: ({ value }) => (value as { name: string }).name },
    { field: 'quantity', headerName: 'Qty Written Off', width: 130, renderCell: ({ value }) => <Typography fontWeight={700} color="error.main">{value}</Typography> },
    { field: 'reason', headerName: 'Reason', width: 130, renderCell: ({ value }) => <Chip label={value?.replace('_', ' ')} color={reasonColor(value as WastageReason)} size="small" /> },
    { field: 'recordedBy', headerName: 'Recorded By', flex: 1, renderCell: ({ value }) => { const u = value as { firstName: string; lastName: string }; return `${u.firstName} ${u.lastName}`; } },
    { field: 'notes', headerName: 'Notes', flex: 1.5, renderCell: ({ value }) => value ?? '—' },
    { field: 'actions', headerName: '', width: 70, sortable: false, renderCell: ({ row }) => <Tooltip title="Delete"><IconButton size="small" color="error" onClick={() => deleteMutation.mutate(row.id)}><DeleteIcon fontSize="small" /></IconButton></Tooltip> },
  ];

  return (
    <Layout title="Wastage / Shrinkage">
      <Box display="flex" gap={2} mb={2} alignItems="center" flexWrap="wrap">
        <FormControl size="small" sx={{ minWidth: 220 }}>
          <InputLabel>Branch</InputLabel>
          <Select value={filterBranch} label="Branch" onChange={(e) => setFilterBranch(e.target.value as number | '')}>
            <MenuItem value="">All Branches</MenuItem>
            {branches.map((b) => <MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}
          </Select>
        </FormControl>
        <Chip icon={<DeleteSweepIcon />} label={`${totalItems} items written off`} color="error" variant="outlined" />
        <Box flex={1} />
        <Button variant="contained" color="warning" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
          Record Wastage
        </Button>
      </Box>

      <Card>
        <DataGrid rows={records} columns={cols} loading={isLoading} autoHeight pageSizeOptions={[25]} initialState={{ pagination: { paginationModel: { pageSize: 25 } } }} disableRowSelectionOnClick sx={{ border: 0 }} getRowId={(r) => r.id} />
      </Card>

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle fontWeight={700}>Record Stock Write-Off</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} mt={1}>
            <Box display="flex" gap={2}>
              <FormControl fullWidth size="small">
                <InputLabel>Branch *</InputLabel>
                <Controller name="branchId" control={control} rules={{ required: true }} render={({ field }) => <Select {...field} label="Branch *">{branches.map((b) => <MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}</Select>} />
              </FormControl>
              <FormControl fullWidth size="small">
                <InputLabel>Product *</InputLabel>
                <Controller name="productId" control={control} rules={{ required: true }} render={({ field }) => <Select {...field} label="Product *">{products.map((p) => <MenuItem key={p.id} value={p.id}>{p.name}</MenuItem>)}</Select>} />
              </FormControl>
            </Box>
            <Box display="flex" gap={2}>
              <TextField fullWidth size="small" label="Quantity *" type="number" inputProps={{ min: 1 }} {...register('quantity', { required: true, min: 1 })} error={!!errors.quantity} />
              <FormControl fullWidth size="small">
                <InputLabel>Reason *</InputLabel>
                <Controller name="reason" control={control} rules={{ required: true }} render={({ field }) => <Select {...field} label="Reason *">{REASONS.map((r) => <MenuItem key={r} value={r}>{r.replace('_', ' ')}</MenuItem>)}</Select>} />
              </FormControl>
            </Box>
            <TextField fullWidth size="small" label="Date *" type="date" InputLabelProps={{ shrink: true }} {...register('wastedAt', { required: true })} />
            <TextField fullWidth size="small" label="Notes" {...register('notes')} placeholder="Describe what happened..." />
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
          <Button variant="contained" color="warning" onClick={handleSubmit((d) => createMutation.mutate(d))} disabled={createMutation.isPending}>
            {createMutation.isPending ? <CircularProgress size={20} color="inherit" /> : 'Record Write-Off'}
          </Button>
        </DialogActions>
      </Dialog>
    </Layout>
  );
};
