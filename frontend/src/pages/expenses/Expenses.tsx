import {
  Box, Card, Button, Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Typography, Chip, CircularProgress, FormControl, InputLabel,
  Select, MenuItem, Paper, IconButton, Tooltip,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import AttachMoneyIcon from '@mui/icons-material/AttachMoney';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm, Controller } from 'react-hook-form';
import { Layout } from '../../components/layout/Layout';
import { expensesApi, type Expense, type ExpenseCategory } from '../../api/expenses';
import { branchesApi } from '../../api/branches';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';

const fmt = (n: number) => `$${Number(n).toFixed(2)}`;

interface ExpenseForm {
  branchId: number | '';
  categoryId: number | '';
  description: string;
  amount: number | '';
  expenseDate: string;
  notes: string;
}

export const Expenses = () => {
  const qc = useQueryClient();
  const { user } = useAuth();
  const { showSuccess, showError } = useToast();
  const [filterBranch, setFilterBranch] = useState<number | ''>(user?.branchId ?? '');
  const [createOpen, setCreateOpen] = useState(false);

  const { data: branches = [] } = useQuery({ queryKey: ['branches'], queryFn: branchesApi.getActive });
  const { data: categories = [] } = useQuery({ queryKey: ['expense-categories'], queryFn: expensesApi.getCategories });

  const { data: expenses = [], isLoading } = useQuery({
    queryKey: ['expenses', filterBranch],
    queryFn: () => filterBranch ? expensesApi.getByBranch(filterBranch) : expensesApi.getAll(),
  });

  const { register, handleSubmit, control, reset, formState: { errors } } = useForm<ExpenseForm>({
    defaultValues: { branchId: user?.branchId ?? '', categoryId: '', expenseDate: new Date().toISOString().split('T')[0] },
  });

  const createMutation = useMutation({
    mutationFn: (data: ExpenseForm) => expensesApi.create({
      branchId: Number(data.branchId), categoryId: Number(data.categoryId),
      description: data.description, amount: Number(data.amount),
      expenseDate: data.expenseDate, notes: data.notes,
    }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['expenses'] }); setCreateOpen(false); reset(); showSuccess('Expense recorded'); },
    onError: (e: unknown) => showError((e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Error'),
  });

  const deleteMutation = useMutation({
    mutationFn: expensesApi.delete,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['expenses'] }); showSuccess('Expense deleted'); },
    onError: () => showError('Failed to delete'),
  });

  const totalExpenses = expenses.reduce((sum, e) => sum + e.amount, 0);

  const cols: GridColDef<Expense>[] = [
    { field: 'expenseDate', headerName: 'Date', width: 110, renderCell: ({ value }) => value },
    { field: 'description', headerName: 'Description', flex: 1.5, minWidth: 180 },
    { field: 'category', headerName: 'Category', flex: 1, minWidth: 130, renderCell: ({ value }) => {
      const cat = value as ExpenseCategory;
      return <Chip label={cat.name} size="small" sx={{ bgcolor: cat.color + '20', color: cat.color, fontWeight: 600 }} />;
    }},
    { field: 'branch', headerName: 'Branch', flex: 1, minWidth: 130, renderCell: ({ value }) => (value as { name: string }).name },
    { field: 'amount', headerName: 'Amount', width: 110, renderCell: ({ value }) => <Typography fontWeight={700}>{fmt(Number(value))}</Typography> },
    { field: 'recordedBy', headerName: 'Recorded By', flex: 1, minWidth: 140, renderCell: ({ value }) => { const u = value as { firstName: string; lastName: string }; return `${u.firstName} ${u.lastName}`; } },
    { field: 'actions', headerName: '', width: 70, sortable: false, renderCell: ({ row }) => (
      <Tooltip title="Delete"><IconButton size="small" color="error" onClick={() => deleteMutation.mutate(row.id)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
    )},
  ];

  return (
    <Layout title="Expenses">
      <Box display="flex" gap={2} mb={2} alignItems="center" flexWrap="wrap">
        <FormControl size="small" sx={{ minWidth: 220 }}>
          <InputLabel>Branch</InputLabel>
          <Select value={filterBranch} label="Branch" onChange={(e) => setFilterBranch(e.target.value as number | '')}>
            <MenuItem value="">All Branches</MenuItem>
            {branches.map((b) => <MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}
          </Select>
        </FormControl>
        <Box flex={1} />
        <Paper sx={{ px: 2, py: 1, display: 'flex', alignItems: 'center', gap: 1, bgcolor: 'error.light' }}>
          <AttachMoneyIcon sx={{ color: 'error.contrastText', fontSize: 20 }} />
          <Typography fontWeight={800} color="error.contrastText">Total: {fmt(totalExpenses)}</Typography>
        </Paper>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>Record Expense</Button>
      </Box>

      <Card>
        <DataGrid rows={expenses} columns={cols} loading={isLoading} autoHeight pageSizeOptions={[25]} initialState={{ pagination: { paginationModel: { pageSize: 25 } } }} disableRowSelectionOnClick sx={{ border: 0 }} getRowId={(r) => r.id} />
      </Card>

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle fontWeight={700}>Record Expense</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} mt={1}>
            <Box display="flex" gap={2}>
              <FormControl fullWidth size="small">
                <InputLabel>Branch *</InputLabel>
                <Controller name="branchId" control={control} rules={{ required: true }}
                  render={({ field }) => <Select {...field} label="Branch *">{branches.map((b) => <MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}</Select>} />
              </FormControl>
              <FormControl fullWidth size="small">
                <InputLabel>Category *</InputLabel>
                <Controller name="categoryId" control={control} rules={{ required: true }}
                  render={({ field }) => <Select {...field} label="Category *">{categories.map((c) => <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>)}</Select>} />
              </FormControl>
            </Box>
            <TextField fullWidth size="small" label="Description *" {...register('description', { required: true })} error={!!errors.description} />
            <Box display="flex" gap={2}>
              <TextField fullWidth size="small" label="Amount ($) *" type="number" inputProps={{ min: 0.01, step: 0.01 }} {...register('amount', { required: true })} error={!!errors.amount} />
              <TextField fullWidth size="small" label="Date *" type="date" InputLabelProps={{ shrink: true }} {...register('expenseDate', { required: true })} />
            </Box>
            <TextField fullWidth size="small" label="Notes" {...register('notes')} />
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmit((d) => createMutation.mutate(d))} disabled={createMutation.isPending}>
            {createMutation.isPending ? <CircularProgress size={20} color="inherit" /> : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>
    </Layout>
  );
};
