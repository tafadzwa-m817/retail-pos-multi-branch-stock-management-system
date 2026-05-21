import {
  Box, Card, Button, Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Typography, Chip, CircularProgress, IconButton, Tooltip,
  Table, TableHead, TableRow, TableCell, TableBody, LinearProgress,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import AddIcon from '@mui/icons-material/Add';
import PaymentIcon from '@mui/icons-material/Payment';
import VisibilityIcon from '@mui/icons-material/Visibility';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { Layout } from '../../components/layout/Layout';
import { creditAccountsApi, type CreditAccount, type CreditTransaction } from '../../api/creditAccounts';
import { customersApi } from '../../api/customers';
import { useToast } from '../../context/ToastContext';

const fmt = (n: number) => `$${Number(n).toFixed(2)}`;

export const CreditAccounts = () => {
  const qc = useQueryClient();
  const { showSuccess, showError } = useToast();
  const [openAccount, setOpenAccount] = useState(false);
  const [viewAccount, setViewAccount] = useState<CreditAccount | null>(null);
  const [repayDialog, setRepayDialog] = useState<CreditAccount | null>(null);

  const { data: accounts = [], isLoading } = useQuery({ queryKey: ['credit-accounts'], queryFn: creditAccountsApi.getAll });
  const { data: customers = [] } = useQuery({ queryKey: ['customers'], queryFn: customersApi.getAll });
  const { data: transactions = [] } = useQuery({
    queryKey: ['credit-transactions', viewAccount?.id],
    queryFn: () => viewAccount ? creditAccountsApi.getTransactions(viewAccount.id) : Promise.resolve([]),
    enabled: !!viewAccount,
  });

  const { register: regOpen, handleSubmit: handleOpen, reset: resetOpen } = useForm<{ customerId: number | ''; creditLimit: number | '' }>();
  const { register: regRepay, handleSubmit: handleRepay, reset: resetRepay } = useForm<{ amount: number | ''; notes: string }>();

  const openMutation = useMutation({
    mutationFn: (d: { customerId: number | ''; creditLimit: number | '' }) =>
      creditAccountsApi.openAccount(Number(d.customerId), Number(d.creditLimit)),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['credit-accounts'] }); setOpenAccount(false); resetOpen(); showSuccess('Credit account opened'); },
    onError: (e: unknown) => showError((e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Error'),
  });

  const repayMutation = useMutation({
    mutationFn: (d: { amount: number | ''; notes: string }) =>
      creditAccountsApi.recordRepayment(repayDialog!.id, Number(d.amount), d.notes),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['credit-accounts'] }); setRepayDialog(null); resetRepay(); showSuccess('Repayment recorded'); },
    onError: (e: unknown) => showError((e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Error'),
  });

  const cols: GridColDef<CreditAccount>[] = [
    { field: 'customer', headerName: 'Customer', flex: 1.5, minWidth: 160, renderCell: ({ value }) => { const c = value as { firstName: string; lastName: string }; return `${c.firstName} ${c.lastName}`; } },
    { field: 'creditLimit', headerName: 'Limit', width: 110, renderCell: ({ value }) => fmt(value) },
    { field: 'currentBalance', headerName: 'Owed', width: 110, renderCell: ({ value }) => <Typography fontWeight={700} color={Number(value) > 0 ? 'error.main' : 'success.main'}>{fmt(value)}</Typography> },
    { field: 'availableCredit', headerName: 'Available', width: 110, renderCell: ({ row }) => fmt(row.creditLimit - row.currentBalance) },
    {
      field: 'utilisation', headerName: 'Utilisation', width: 140,
      renderCell: ({ row }) => {
        const pct = row.creditLimit > 0 ? (row.currentBalance / row.creditLimit) * 100 : 0;
        return <Box width="100%"><LinearProgress variant="determinate" value={Math.min(pct, 100)} color={pct > 80 ? 'error' : pct > 50 ? 'warning' : 'success'} /><Typography variant="caption">{pct.toFixed(0)}%</Typography></Box>;
      },
    },
    { field: 'active', headerName: 'Status', width: 90, renderCell: ({ value }) => <Chip label={value ? 'Active' : 'Closed'} color={value ? 'success' : 'default'} size="small" /> },
    {
      field: 'actions', headerName: '', width: 110, sortable: false,
      renderCell: ({ row }) => (
        <Box display="flex" gap={0.5}>
          <Tooltip title="View transactions"><IconButton size="small" onClick={() => setViewAccount(row)}><VisibilityIcon fontSize="small" /></IconButton></Tooltip>
          {row.currentBalance > 0 && <Tooltip title="Record repayment"><IconButton size="small" color="success" onClick={() => setRepayDialog(row)}><PaymentIcon fontSize="small" /></IconButton></Tooltip>}
        </Box>
      ),
    },
  ];

  return (
    <Layout title="Credit Accounts">
      <Box display="flex" justifyContent="flex-end" mb={2}>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpenAccount(true)}>Open Account</Button>
      </Box>
      <Card>
        <DataGrid rows={accounts} columns={cols} loading={isLoading} autoHeight pageSizeOptions={[20]} initialState={{ pagination: { paginationModel: { pageSize: 20 } } }} disableRowSelectionOnClick sx={{ border: 0 }} />
      </Card>

      {/* Open Account */}
      <Dialog open={openAccount} onClose={() => setOpenAccount(false)} maxWidth="xs" fullWidth>
        <DialogTitle fontWeight={700}>Open Credit Account</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} mt={1}>
            <TextField fullWidth size="small" label="Customer ID *" type="number" {...regOpen('customerId', { required: true })} helperText="Enter the customer ID" />
            <TextField fullWidth size="small" label="Credit Limit ($) *" type="number" inputProps={{ min: 1, step: 10 }} {...regOpen('creditLimit', { required: true })} />
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setOpenAccount(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleOpen((d) => openMutation.mutate(d))} disabled={openMutation.isPending}>Open</Button>
        </DialogActions>
      </Dialog>

      {/* Repayment */}
      <Dialog open={!!repayDialog} onClose={() => setRepayDialog(null)} maxWidth="xs" fullWidth>
        <DialogTitle fontWeight={700}>Record Repayment</DialogTitle>
        <DialogContent>
          {repayDialog && <Box p={1.5} bgcolor="action.hover" borderRadius={2} mb={2}>
            <Typography fontWeight={600}>{repayDialog.customer.firstName} {repayDialog.customer.lastName}</Typography>
            <Typography variant="caption" color="error.main">Outstanding: {fmt(repayDialog.currentBalance)}</Typography>
          </Box>}
          <Box display="flex" flexDirection="column" gap={2}>
            <TextField fullWidth size="small" label="Amount ($) *" type="number" inputProps={{ min: 0.01, step: 0.01 }} {...regRepay('amount', { required: true })} />
            <TextField fullWidth size="small" label="Notes" {...regRepay('notes')} />
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setRepayDialog(null)}>Cancel</Button>
          <Button variant="contained" color="success" onClick={handleRepay((d) => repayMutation.mutate(d))} disabled={repayMutation.isPending}>Record</Button>
        </DialogActions>
      </Dialog>

      {/* Transaction History */}
      <Dialog open={!!viewAccount} onClose={() => setViewAccount(null)} maxWidth="sm" fullWidth>
        <DialogTitle fontWeight={700}>
          Credit Account — {viewAccount?.customer.firstName} {viewAccount?.customer.lastName}
        </DialogTitle>
        <DialogContent>
          {viewAccount && <Box mb={2} display="flex" gap={2}>
            <Chip label={`Limit: ${fmt(viewAccount.creditLimit)}`} />
            <Chip label={`Owed: ${fmt(viewAccount.currentBalance)}`} color={viewAccount.currentBalance > 0 ? 'error' : 'success'} />
          </Box>}
          <Table size="small">
            <TableHead><TableRow><TableCell sx={{ fontWeight: 700 }}>Date</TableCell><TableCell sx={{ fontWeight: 700 }}>Type</TableCell><TableCell align="right" sx={{ fontWeight: 700 }}>Amount</TableCell><TableCell sx={{ fontWeight: 700 }}>Notes</TableCell></TableRow></TableHead>
            <TableBody>
              {transactions.map((t) => (
                <TableRow key={t.id}>
                  <TableCell>{new Date(t.createdAt).toLocaleDateString()}</TableCell>
                  <TableCell><Chip label={t.type} color={t.type === 'DEBIT' ? 'error' : 'success'} size="small" /></TableCell>
                  <TableCell align="right" sx={{ fontWeight: 700, color: t.type === 'DEBIT' ? 'error.main' : 'success.main' }}>{t.type === 'DEBIT' ? '+' : '-'}{fmt(t.amount)}</TableCell>
                  <TableCell>{t.notes ?? '—'}</TableCell>
                </TableRow>
              ))}
              {transactions.length === 0 && <TableRow><TableCell colSpan={4} align="center" sx={{ py: 3, color: 'text.secondary' }}>No transactions yet</TableCell></TableRow>}
            </TableBody>
          </Table>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}><Button onClick={() => setViewAccount(null)}>Close</Button></DialogActions>
      </Dialog>
    </Layout>
  );
};
