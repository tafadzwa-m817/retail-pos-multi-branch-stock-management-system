import {
  Box, Card, CardContent, Typography, Button, Dialog, DialogTitle,
  DialogContent, DialogActions, TextField, FormControl, InputLabel, Select,
  MenuItem, LinearProgress, Chip, CircularProgress, Grid,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import TrackChangesIcon from '@mui/icons-material/TrackChanges';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm, Controller } from 'react-hook-form';
import { Layout } from '../../components/layout/Layout';
import { salesTargetsApi, type TargetProgress } from '../../api/salesTargets';
import { branchesApi } from '../../api/branches';
import { useToast } from '../../context/ToastContext';

const fmt = (n: number) => `$${Number(n).toFixed(2)}`;
const MONTHS = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
const currentYear = new Date().getFullYear();
const currentMonth = new Date().getMonth() + 1;

interface TargetForm { branchId: number | ''; targetAmount: number | ''; month: number; year: number; }

export const SalesTargets = () => {
  const qc = useQueryClient();
  const { showSuccess, showError } = useToast();
  const [open, setOpen] = useState(false);

  const { data: progress = [], isLoading: loadingProgress } = useQuery({ queryKey: ['target-progress'], queryFn: salesTargetsApi.getProgress });
  const { data: branches = [] } = useQuery({ queryKey: ['branches'], queryFn: branchesApi.getActive });

  const { register, handleSubmit, control, reset } = useForm<TargetForm>({
    defaultValues: { month: currentMonth, year: currentYear, branchId: '', targetAmount: '' },
  });

  const setMutation = useMutation({
    mutationFn: (d: TargetForm) => salesTargetsApi.setTarget({ branchId: Number(d.branchId), targetAmount: Number(d.targetAmount), month: d.month, year: d.year }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['target-progress'] }); setOpen(false); reset(); showSuccess('Target set'); },
    onError: (e: unknown) => showError((e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Error'),
  });

  const getProgressColor = (pct: number) => pct >= 100 ? 'success' : pct >= 75 ? 'warning' : 'error';

  return (
    <Layout title="Sales Targets">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={1} color="text.secondary">
          <TrackChangesIcon fontSize="small" />
          <Typography variant="body2">
            Current month: <strong>{MONTHS[currentMonth - 1]} {currentYear}</strong>
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpen(true)}>Set Target</Button>
      </Box>

      {loadingProgress ? <CircularProgress /> : (
        <Grid container spacing={3}>
          {progress.length === 0 ? (
            <Grid item xs={12}>
              <Card><CardContent sx={{ textAlign: 'center', py: 6 }}>
                <TrackChangesIcon sx={{ fontSize: 48, mb: 2, color: 'text.secondary', opacity: 0.4 }} />
                <Typography color="text.secondary">No targets set for this month yet</Typography>
                <Button variant="contained" sx={{ mt: 2 }} onClick={() => setOpen(true)}>Set First Target</Button>
              </CardContent></Card>
            </Grid>
          ) : (
            progress.map((p) => <Grid item xs={12} md={6} lg={4} key={p.branchId}><TargetCard data={p} /></Grid>)
          )}
        </Grid>
      )}

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle fontWeight={700}>Set Monthly Target</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} mt={1}>
            <FormControl fullWidth size="small">
              <InputLabel>Branch *</InputLabel>
              <Controller name="branchId" control={control} rules={{ required: true }} render={({ field }) => <Select {...field} label="Branch *">{branches.map((b) => <MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}</Select>} />
            </FormControl>
            <TextField fullWidth size="small" label="Target Revenue ($)" type="number" inputProps={{ min: 1, step: 100 }} {...register('targetAmount', { required: true })} />
            <Box display="flex" gap={2}>
              <FormControl fullWidth size="small">
                <InputLabel>Month</InputLabel>
                <Controller name="month" control={control} render={({ field }) => <Select {...field} label="Month">{MONTHS.map((m, i) => <MenuItem key={i + 1} value={i + 1}>{m}</MenuItem>)}</Select>} />
              </FormControl>
              <TextField fullWidth size="small" label="Year" type="number" {...register('year')} />
            </Box>
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmit((d) => setMutation.mutate(d))} disabled={setMutation.isPending}>
            {setMutation.isPending ? <CircularProgress size={20} color="inherit" /> : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>
    </Layout>
  );
};

const TargetCard = ({ data }: { data: TargetProgress }) => {
  const pct = Math.min(100, Number(data.progressPct));
  const color = pct >= 100 ? 'success' : pct >= 75 ? 'warning' : 'error';
  return (
    <Card>
      <CardContent>
        <Typography variant="subtitle1" fontWeight={700} mb={1}>{data.branchName}</Typography>
        <Box display="flex" justifyContent="space-between" mb={0.5}>
          <Typography variant="caption" color="text.secondary">Achieved</Typography>
          <Typography variant="caption" fontWeight={700} color={color + '.main'}>{fmt(data.achieved)}</Typography>
        </Box>
        <Box display="flex" justifyContent="space-between" mb={1}>
          <Typography variant="caption" color="text.secondary">Target</Typography>
          <Typography variant="caption">{fmt(data.target)}</Typography>
        </Box>
        <LinearProgress variant="determinate" value={pct} color={color as 'success' | 'warning' | 'error'} sx={{ height: 10, borderRadius: 5, mb: 1 }} />
        <Box display="flex" justifyContent="space-between" alignItems="center">
          <Typography variant="h6" fontWeight={800} color={color + '.main'}>{pct}%</Typography>
          <Chip label={pct >= 100 ? 'TARGET MET!' : pct >= 75 ? 'On Track' : 'Behind'} color={color as 'success' | 'warning' | 'error'} size="small" />
        </Box>
      </CardContent>
    </Card>
  );
};
