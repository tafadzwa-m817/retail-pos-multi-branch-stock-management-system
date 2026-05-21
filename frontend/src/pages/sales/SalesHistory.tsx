import {
  Box, Card, Chip, Button, Dialog, DialogTitle, DialogContent, DialogActions,
  Typography, FormControl, InputLabel, Select, MenuItem, TextField, Alert,
  IconButton, Tooltip,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import ReceiptIcon from '@mui/icons-material/Receipt';
import BlockIcon from '@mui/icons-material/Block';
import PrintIcon from '@mui/icons-material/Print';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Layout } from '../../components/layout/Layout';
import { Receipt, printReceipt } from '../../components/pos/Receipt';
import { salesApi } from '../../api/sales';
import { branchesApi } from '../../api/branches';
import type { SaleResponse } from '../../types';

const statusColor = (s: string): 'success' | 'error' | 'warning' | 'default' =>
  ({ COMPLETED: 'success', VOIDED: 'error', REFUNDED: 'warning' } as const)[s] ?? 'default';

const fmt = (n: number) => `$${n.toFixed(2)}`;

export const SalesHistory = () => {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [branchId, setBranchId] = useState<number | ''>('');
  const [viewSale, setViewSale] = useState<SaleResponse | null>(null);
  const [voidError, setVoidError] = useState('');

  const { data: branches = [] } = useQuery({ queryKey: ['branches'], queryFn: branchesApi.getActive });

  const { data: salesPage, isLoading } = useQuery({
    queryKey: ['sales', page],
    queryFn: () => salesApi.getAll(page, 25),
  });

  const voidMutation = useMutation({
    mutationFn: salesApi.voidSale,
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['sales'] });
      setViewSale(updated);
      setVoidError('');
    },
    onError: (err: unknown) =>
      setVoidError((err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Failed to void sale'),
  });

  const columns: GridColDef<SaleResponse>[] = [
    { field: 'id', headerName: '#', width: 70 },
    { field: 'branchName', headerName: 'Branch', flex: 1, minWidth: 140 },
    { field: 'cashierName', headerName: 'Cashier', flex: 1, minWidth: 140 },
    {
      field: 'customerName', headerName: 'Customer', flex: 1, minWidth: 130,
      renderCell: ({ value }) => value ?? <Typography color="text.disabled" variant="caption">Walk-in</Typography>,
    },
    {
      field: 'totalAmount', headerName: 'Total', width: 100,
      renderCell: ({ value }) => <Typography fontWeight={700}>{fmt(value)}</Typography>,
    },
    { field: 'paymentMethod', headerName: 'Payment', width: 120, renderCell: ({ value }) => <Chip label={value?.replace('_', ' ')} size="small" variant="outlined" /> },
    {
      field: 'status', headerName: 'Status', width: 110,
      renderCell: ({ value }) => <Chip label={value} color={statusColor(value)} size="small" />,
    },
    {
      field: 'createdAt', headerName: 'Date', flex: 1, minWidth: 160,
      renderCell: ({ value }) => new Date(value).toLocaleString(),
    },
    {
      field: 'actions', headerName: '', width: 100, sortable: false,
      renderCell: ({ row }) => (
        <Box display="flex" gap={0.5}>
          <Tooltip title="View receipt">
            <IconButton size="small" onClick={() => { setViewSale(row); setVoidError(''); }}>
              <ReceiptIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Print receipt">
            <IconButton size="small" onClick={() => printReceipt(row)}>
              <PrintIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Box>
      ),
    },
  ];

  const rows = salesPage?.content ?? [];
  const totalRows = salesPage?.totalElements ?? 0;

  return (
    <Layout title="Sales History">
      <Box display="flex" gap={2} mb={2} alignItems="center">
        <FormControl size="small" sx={{ minWidth: 200 }}>
          <InputLabel>Filter by Branch</InputLabel>
          <Select value={branchId} label="Filter by Branch" onChange={(e) => setBranchId(e.target.value as number | '')}>
            <MenuItem value="">All Branches</MenuItem>
            {branches.map((b) => <MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}
          </Select>
        </FormControl>
        <Box flex={1} />
        <Typography variant="body2" color="text.secondary">{totalRows} total sales</Typography>
      </Box>

      <Card>
        <DataGrid
          rows={rows}
          columns={columns}
          loading={isLoading}
          rowCount={totalRows}
          paginationMode="server"
          paginationModel={{ page, pageSize: 25 }}
          onPaginationModelChange={(m) => setPage(m.page)}
          pageSizeOptions={[25]}
          disableRowSelectionOnClick
          autoHeight
          sx={{ border: 0 }}
        />
      </Card>

      {/* Sale Detail / Receipt Dialog */}
      <Dialog open={!!viewSale} onClose={() => setViewSale(null)} maxWidth="sm" fullWidth>
        <DialogTitle fontWeight={700} display="flex" alignItems="center" gap={1}>
          <ReceiptIcon color="primary" />
          Sale #{viewSale?.id}
        </DialogTitle>
        <DialogContent>
          {voidError && <Alert severity="error" sx={{ mb: 2 }}>{voidError}</Alert>}
          {viewSale && <Receipt sale={viewSale} showPrintButton={false} />}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2, justifyContent: 'space-between' }}>
          <Box display="flex" gap={1}>
            {viewSale?.status === 'COMPLETED' && (
              <Button
                color="error"
                startIcon={<BlockIcon />}
                onClick={() => voidMutation.mutate(viewSale!.id)}
                disabled={voidMutation.isPending}
              >
                Void Sale
              </Button>
            )}
          </Box>
          <Box display="flex" gap={1}>
            {viewSale && (
              <Button startIcon={<PrintIcon />} variant="outlined" onClick={() => printReceipt(viewSale)}>
                Print
              </Button>
            )}
            <Button onClick={() => setViewSale(null)}>Close</Button>
          </Box>
        </DialogActions>
      </Dialog>
    </Layout>
  );
};
