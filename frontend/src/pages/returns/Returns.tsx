import {
  Box, Card, Button, Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Typography, Chip, Alert, CircularProgress, Table, TableHead,
  TableRow, TableCell, TableBody, IconButton, Tooltip,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import AddIcon from '@mui/icons-material/Add';
import VisibilityIcon from '@mui/icons-material/Visibility';
import RemoveIcon from '@mui/icons-material/Remove';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Layout } from '../../components/layout/Layout';
import { returnsApi } from '../../api/returns';
import { salesApi } from '../../api/sales';
import { useToast } from '../../context/ToastContext';
import type { ReturnResponse, SaleResponse } from '../../types';

const fmt = (n: number) => `$${Number(n).toFixed(2)}`;

export const Returns = () => {
  const qc = useQueryClient();
  const { showSuccess, showError } = useToast();
  const [viewReturn, setViewReturn] = useState<ReturnResponse | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [saleIdInput, setSaleIdInput] = useState('');
  const [lookupSale, setLookupSale] = useState<SaleResponse | null>(null);
  const [lookupError, setLookupError] = useState('');
  const [reason, setReason] = useState('');
  const [returnItems, setReturnItems] = useState<{ saleItemId: number; quantity: number; max: number; productName: string }[]>([]);

  const { data: returns = [], isLoading } = useQuery({ queryKey: ['returns'], queryFn: returnsApi.getAll });

  const processReturn = useMutation({
    mutationFn: returnsApi.processReturn,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['returns'] }); closeCreate(); showSuccess('Return processed successfully'); },
    onError: (e: unknown) => showError((e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Return failed'),
  });

  const closeCreate = () => {
    setCreateOpen(false); setSaleIdInput(''); setLookupSale(null); setLookupError(''); setReason(''); setReturnItems([]);
  };

  const handleLookupSale = async () => {
    try {
      const sale = await salesApi.getById(Number(saleIdInput));
      setLookupSale(sale);
      setReturnItems(sale.items.map((i) => ({ saleItemId: i.id, quantity: 0, max: i.quantity, productName: i.productName })));
      setLookupError('');
    } catch {
      setLookupError(`Sale #${saleIdInput} not found`);
      setLookupSale(null);
    }
  };

  const updateQty = (saleItemId: number, delta: number) => {
    setReturnItems((prev) => prev.map((i) => i.saleItemId === saleItemId ? { ...i, quantity: Math.max(0, Math.min(i.max, i.quantity + delta)) } : i));
  };

  const handleSubmitReturn = () => {
    const items = returnItems.filter((i) => i.quantity > 0).map((i) => ({ saleItemId: i.saleItemId, quantity: i.quantity }));
    if (items.length === 0) { showError('Select at least one item to return'); return; }
    if (!reason.trim()) { showError('Please provide a return reason'); return; }
    processReturn.mutate({ originalSaleId: lookupSale!.id, reason, items });
  };

  const cols: GridColDef<ReturnResponse>[] = [
    { field: 'id', headerName: '#', width: 70 },
    { field: 'originalSaleId', headerName: 'Sale #', width: 90, renderCell: ({ value }) => <Chip label={`#${value}`} size="small" variant="outlined" /> },
    { field: 'branchName', headerName: 'Branch', flex: 1, minWidth: 140 },
    { field: 'processedByName', headerName: 'Processed By', flex: 1, minWidth: 140 },
    { field: 'reason', headerName: 'Reason', flex: 1.5, minWidth: 160 },
    { field: 'totalRefundAmount', headerName: 'Refund', width: 110, renderCell: ({ value }) => <Typography fontWeight={700} color="error.main">{fmt(value)}</Typography> },
    { field: 'createdAt', headerName: 'Date', flex: 1, minWidth: 150, renderCell: ({ value }) => new Date(value).toLocaleString() },
    { field: 'actions', headerName: '', width: 80, sortable: false, renderCell: ({ row }) => <Tooltip title="View details"><IconButton size="small" onClick={() => setViewReturn(row)}><VisibilityIcon fontSize="small" /></IconButton></Tooltip> },
  ];

  return (
    <Layout title="Product Returns">
      <Box display="flex" justifyContent="flex-end" mb={2}>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>Process Return</Button>
      </Box>

      <Card>
        <DataGrid rows={returns} columns={cols} loading={isLoading} autoHeight pageSizeOptions={[20]} initialState={{ pagination: { paginationModel: { pageSize: 20 } } }} disableRowSelectionOnClick sx={{ border: 0 }} />
      </Card>

      {/* View Return Dialog */}
      <Dialog open={!!viewReturn} onClose={() => setViewReturn(null)} maxWidth="sm" fullWidth>
        <DialogTitle fontWeight={700}>Return #{viewReturn?.id} — Sale #{viewReturn?.originalSaleId}</DialogTitle>
        <DialogContent>
          {viewReturn && (
            <Box>
              <Box display="flex" gap={2} flexWrap="wrap" mb={2}>
                <Box><Typography variant="caption" color="text.secondary">Branch</Typography><Typography fontWeight={600}>{viewReturn.branchName}</Typography></Box>
                <Box><Typography variant="caption" color="text.secondary">Processed By</Typography><Typography fontWeight={600}>{viewReturn.processedByName}</Typography></Box>
                <Box><Typography variant="caption" color="text.secondary">Date</Typography><Typography fontWeight={600}>{new Date(viewReturn.createdAt).toLocaleString()}</Typography></Box>
              </Box>
              <Typography variant="body2" mb={2}><strong>Reason:</strong> {viewReturn.reason}</Typography>
              <Table size="small">
                <TableHead><TableRow><TableCell sx={{ fontWeight: 700 }}>Product</TableCell><TableCell align="right" sx={{ fontWeight: 700 }}>Qty</TableCell><TableCell align="right" sx={{ fontWeight: 700 }}>Refund</TableCell></TableRow></TableHead>
                <TableBody>
                  {viewReturn.items.map((i) => (
                    <TableRow key={i.id}><TableCell>{i.productName}</TableCell><TableCell align="right">{i.quantity}</TableCell><TableCell align="right" sx={{ fontWeight: 700 }}>{fmt(i.totalRefundAmount)}</TableCell></TableRow>
                  ))}
                  <TableRow><TableCell colSpan={2}><strong>Total Refund</strong></TableCell><TableCell align="right"><Typography fontWeight={800} color="error.main">{fmt(viewReturn.totalRefundAmount)}</Typography></TableCell></TableRow>
                </TableBody>
              </Table>
            </Box>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}><Button onClick={() => setViewReturn(null)}>Close</Button></DialogActions>
      </Dialog>

      {/* Create Return Dialog */}
      <Dialog open={createOpen} onClose={closeCreate} maxWidth="sm" fullWidth>
        <DialogTitle fontWeight={700}>Process Return</DialogTitle>
        <DialogContent>
          <Box display="flex" gap={1} mt={1} mb={2}>
            <TextField size="small" label="Original Sale ID" value={saleIdInput} onChange={(e) => setSaleIdInput(e.target.value)} type="number" sx={{ flex: 1 }} />
            <Button variant="outlined" onClick={handleLookupSale} disabled={!saleIdInput}>Lookup</Button>
          </Box>

          {lookupError && <Alert severity="error" sx={{ mb: 2 }}>{lookupError}</Alert>}

          {lookupSale && (
            <Box>
              <Box p={1.5} bgcolor="action.hover" borderRadius={2} mb={2}>
                <Typography variant="body2" fontWeight={700}>Sale #{lookupSale.id} · {lookupSale.branchName} · {fmt(lookupSale.totalAmount)}</Typography>
                <Typography variant="caption" color="text.secondary">{new Date(lookupSale.createdAt).toLocaleString()} · {lookupSale.cashierName}</Typography>
              </Box>

              <Typography variant="subtitle2" fontWeight={700} mb={1}>Select Items to Return</Typography>
              {returnItems.map((item) => (
                <Box key={item.saleItemId} display="flex" justifyContent="space-between" alignItems="center" py={0.75} borderBottom="1px solid" sx={{ borderColor: 'divider' }}>
                  <Typography variant="body2">{item.productName} <Typography component="span" variant="caption" color="text.secondary">(max {item.max})</Typography></Typography>
                  <Box display="flex" alignItems="center" gap={0.5}>
                    <IconButton size="small" onClick={() => updateQty(item.saleItemId, -1)} disabled={item.quantity === 0}><RemoveIcon fontSize="small" /></IconButton>
                    <Typography variant="body2" fontWeight={700} minWidth={24} textAlign="center">{item.quantity}</Typography>
                    <IconButton size="small" onClick={() => updateQty(item.saleItemId, 1)} disabled={item.quantity >= item.max}><AddIcon fontSize="small" /></IconButton>
                  </Box>
                </Box>
              ))}

              <TextField fullWidth size="small" label="Return Reason *" value={reason} onChange={(e) => setReason(e.target.value)} sx={{ mt: 2 }} placeholder="e.g. Defective, Wrong item, Customer changed mind" />
            </Box>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={closeCreate}>Cancel</Button>
          <Button variant="contained" color="warning" onClick={handleSubmitReturn} disabled={!lookupSale || processReturn.isPending}>
            {processReturn.isPending ? <CircularProgress size={20} color="inherit" /> : 'Process Return'}
          </Button>
        </DialogActions>
      </Dialog>
    </Layout>
  );
};
