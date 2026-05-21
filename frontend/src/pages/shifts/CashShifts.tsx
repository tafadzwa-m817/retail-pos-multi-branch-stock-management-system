import {
  Box, Card, Button, Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Typography, Chip, Alert, CircularProgress, FormControl,
  InputLabel, Select, MenuItem, Divider,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import LockOpenIcon from '@mui/icons-material/LockOpen';
import LockIcon from '@mui/icons-material/Lock';
import AddIcon from '@mui/icons-material/Add';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { Layout } from '../../components/layout/Layout';
import { cashShiftsApi } from '../../api/cashShifts';
import { branchesApi } from '../../api/branches';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import type { CashShiftResponse } from '../../types';

const fmt = (n?: number) => n != null ? `$${Number(n).toFixed(2)}` : '—';

export const CashShifts = () => {
  const qc = useQueryClient();
  const { user } = useAuth();
  const { showSuccess, showError } = useToast();
  const [selectedBranchId, setSelectedBranchId] = useState<number | ''>(user?.branchId ?? '');
  const [openShiftDialog, setOpenShiftDialog] = useState(false);
  const [closeShiftDialog, setCloseShiftDialog] = useState<CashShiftResponse | null>(null);

  const { data: branches = [] } = useQuery({ queryKey: ['branches'], queryFn: branchesApi.getActive });

  const { data: shifts = [], isLoading } = useQuery({
    queryKey: ['shifts', selectedBranchId],
    queryFn: () => selectedBranchId ? cashShiftsApi.getByBranch(selectedBranchId) : Promise.resolve([]),
    enabled: !!selectedBranchId,
  });

  const { register: regOpen, handleSubmit: handleOpen, reset: resetOpen } = useForm<{ openingFloat: number; notes: string }>();
  const { register: regClose, handleSubmit: handleClose, reset: resetClose } = useForm<{ closingCash: number; notes: string }>();

  const openMutation = useMutation({
    mutationFn: (data: { openingFloat: number; notes: string }) =>
      cashShiftsApi.openShift({ branchId: Number(selectedBranchId), openingFloat: Number(data.openingFloat), notes: data.notes }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['shifts'] }); setOpenShiftDialog(false); resetOpen(); showSuccess('Shift opened'); },
    onError: (e: unknown) => showError((e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Failed to open shift'),
  });

  const closeMutation = useMutation({
    mutationFn: (data: { id: number; closingCash: number; notes: string }) =>
      cashShiftsApi.closeShift(data.id, { closingCash: Number(data.closingCash), notes: data.notes }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['shifts'] }); setCloseShiftDialog(null); resetClose(); showSuccess('Shift closed'); },
    onError: (e: unknown) => showError((e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Failed to close shift'),
  });

  const openShift = shifts.find((s) => s.status === 'OPEN');

  const cols: GridColDef<CashShiftResponse>[] = [
    { field: 'id', headerName: '#', width: 70 },
    { field: 'openedByName', headerName: 'Opened By', flex: 1, minWidth: 140 },
    { field: 'openingFloat', headerName: 'Float', width: 110, renderCell: ({ value }) => <Typography fontWeight={600}>{fmt(value)}</Typography> },
    { field: 'totalSalesAmount', headerName: 'Sales', width: 110, renderCell: ({ value }) => fmt(value) },
    { field: 'totalTransactions', headerName: 'Txns', width: 80 },
    { field: 'closingCash', headerName: 'Closing Cash', width: 120, renderCell: ({ value }) => fmt(value) },
    {
      field: 'variance', headerName: 'Variance', width: 110,
      renderCell: ({ value }) => value != null ? (
        <Typography fontWeight={700} color={Number(value) >= 0 ? 'success.main' : 'error.main'}>{fmt(value)}</Typography>
      ) : '—',
    },
    {
      field: 'status', headerName: 'Status', width: 100,
      renderCell: ({ value }) => <Chip label={value} color={value === 'OPEN' ? 'success' : 'default'} size="small" icon={value === 'OPEN' ? <LockOpenIcon /> : <LockIcon />} />,
    },
    { field: 'openedAt', headerName: 'Opened', flex: 1, minWidth: 150, renderCell: ({ value }) => new Date(value).toLocaleString() },
    { field: 'closedAt', headerName: 'Closed', flex: 1, minWidth: 150, renderCell: ({ value }) => value ? new Date(value).toLocaleString() : '—' },
    {
      field: 'actions', headerName: '', width: 100, sortable: false,
      renderCell: ({ row }) => row.status === 'OPEN' ? (
        <Button size="small" color="error" startIcon={<LockIcon />} onClick={() => setCloseShiftDialog(row)}>Close</Button>
      ) : null,
    },
  ];

  return (
    <Layout title="Cash Shifts">
      <Box display="flex" gap={2} mb={2} alignItems="center" flexWrap="wrap">
        <FormControl size="small" sx={{ minWidth: 220 }}>
          <InputLabel>Branch</InputLabel>
          <Select value={selectedBranchId} label="Branch" onChange={(e) => setSelectedBranchId(e.target.value as number | '')}>
            {branches.map((b) => <MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}
          </Select>
        </FormControl>

        {openShift && (
          <Chip icon={<LockOpenIcon />} label={`Shift open since ${new Date(openShift.openedAt).toLocaleTimeString()}`} color="success" />
        )}

        <Box flex={1} />
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          disabled={!!openShift || !selectedBranchId}
          onClick={() => setOpenShiftDialog(true)}
        >
          Open Shift
        </Button>
        {openShift && (
          <Button variant="outlined" color="error" startIcon={<LockIcon />} onClick={() => setCloseShiftDialog(openShift)}>
            Close Current Shift
          </Button>
        )}
      </Box>

      <Card>
        <DataGrid
          rows={shifts} columns={cols} loading={isLoading} autoHeight
          pageSizeOptions={[20]} initialState={{ pagination: { paginationModel: { pageSize: 20 } } }}
          disableRowSelectionOnClick sx={{ border: 0 }}
        />
      </Card>

      {/* Open Shift Dialog */}
      <Dialog open={openShiftDialog} onClose={() => setOpenShiftDialog(false)} maxWidth="xs" fullWidth>
        <DialogTitle fontWeight={700}>Open New Shift</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} mt={1}>
            <TextField fullWidth size="small" label="Opening Float ($)" type="number" inputProps={{ min: 0, step: 0.01 }} {...regOpen('openingFloat', { required: true, min: 0 })} />
            <TextField fullWidth size="small" label="Notes (optional)" {...regOpen('notes')} />
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setOpenShiftDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleOpen((d) => openMutation.mutate(d))} disabled={openMutation.isPending}>
            {openMutation.isPending ? <CircularProgress size={20} color="inherit" /> : 'Open Shift'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Close Shift Dialog */}
      <Dialog open={!!closeShiftDialog} onClose={() => setCloseShiftDialog(null)} maxWidth="xs" fullWidth>
        <DialogTitle fontWeight={700}>Close Shift</DialogTitle>
        <DialogContent>
          {closeShiftDialog && (
            <Box mb={2} p={1.5} bgcolor="action.hover" borderRadius={2}>
              <Typography variant="caption" color="text.secondary">Opening float</Typography>
              <Typography fontWeight={700}>{fmt(closeShiftDialog.openingFloat)}</Typography>
              <Typography variant="caption" color="text.secondary">Sales during shift</Typography>
              <Typography fontWeight={700}>{fmt(closeShiftDialog.totalSalesAmount)}</Typography>
              <Divider sx={{ my: 1 }} />
              <Typography variant="caption" color="text.secondary">Expected closing cash</Typography>
              <Typography fontWeight={800} color="primary.main">
                {fmt(closeShiftDialog.openingFloat + closeShiftDialog.totalSalesAmount)}
              </Typography>
            </Box>
          )}
          <Box display="flex" flexDirection="column" gap={2}>
            <TextField fullWidth size="small" label="Actual Closing Cash ($)" type="number" inputProps={{ min: 0, step: 0.01 }} {...regClose('closingCash', { required: true })} />
            <TextField fullWidth size="small" label="Notes (optional)" {...regClose('notes')} />
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setCloseShiftDialog(null)}>Cancel</Button>
          <Button variant="contained" color="error" onClick={handleClose((d) => closeMutation.mutate({ id: closeShiftDialog!.id, ...d }))} disabled={closeMutation.isPending}>
            {closeMutation.isPending ? <CircularProgress size={20} color="inherit" /> : 'Close Shift'}
          </Button>
        </DialogActions>
      </Dialog>
    </Layout>
  );
};
