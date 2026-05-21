import {
  Box, Card, CardContent, Typography, Grid, FormControl, InputLabel,
  Select, MenuItem, TextField, Button, Divider, Table, TableHead,
  TableRow, TableCell, TableBody, Alert, CircularProgress, Tabs, Tab, Chip,
} from '@mui/material';
import DownloadIcon from '@mui/icons-material/Download';
import BarChartIcon from '@mui/icons-material/BarChart';
import InventoryIcon from '@mui/icons-material/Inventory';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import TableChartIcon from '@mui/icons-material/TableChart';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  ResponsiveContainer, ComposedChart, Line, Bar, XAxis, YAxis,
  CartesianGrid, Tooltip as ReTooltip, Legend,
} from 'recharts';
import { Layout } from '../../components/layout/Layout';
import { reportsApi } from '../../api/reports';
import { inventoryApi } from '../../api/inventory';
import { branchesApi } from '../../api/branches';
import { expensesApi } from '../../api/expenses';

const fmt = (n: number) => `$${Number(n).toFixed(2)}`;

const today = () => {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return d.toISOString();
};
const now = () => new Date().toISOString();

export const Reports = () => {
  const [tab, setTab] = useState(0);

  // Sales report state
  const [branchId, setBranchId] = useState<number | ''>('');
  const [start, setStart] = useState(today().slice(0, 16));
  const [end, setEnd] = useState(now().slice(0, 16));
  const [downloading, setDownloading] = useState(false);

  // Inventory report state
  const [invBranchId, setInvBranchId] = useState<number | ''>('');

  const { data: branches = [] } = useQuery({ queryKey: ['branches'], queryFn: branchesApi.getActive });

  const { data: salesReport, isLoading: salesLoading, refetch: refetchSales, error: salesError } = useQuery({
    queryKey: ['report-sales', branchId, start, end],
    queryFn: () => reportsApi.getSalesReport({
      branchId: branchId || undefined,
      start: new Date(start).toISOString(),
      end: new Date(end).toISOString(),
    }),
    enabled: false,
  });

  const { data: inventory = [], isLoading: invLoading } = useQuery({
    queryKey: ['report-inventory', invBranchId],
    queryFn: () => invBranchId ? inventoryApi.getByBranch(invBranchId) : inventoryApi.getAll(),
  });

  const handleSalesExport = async (type: 'csv' | 'pdf' | 'excel') => {
    setDownloading(true);
    const params = { branchId: branchId || undefined, start: new Date(start).toISOString(), end: new Date(end).toISOString() };
    if (type === 'csv') await reportsApi.exportSalesCSV(params);
    else if (type === 'pdf') await reportsApi.downloadSalesPdf(params);
    else await reportsApi.downloadSalesExcel(params);
    setDownloading(false);
  };

  const handleInventoryExport = async (type: 'csv' | 'pdf' | 'excel') => {
    if (!invBranchId) return;
    setDownloading(true);
    if (type === 'csv') await reportsApi.exportInventoryCSV(invBranchId);
    else if (type === 'pdf') await reportsApi.downloadInventoryPdf(invBranchId);
    else await reportsApi.downloadInventoryExcel(invBranchId);
    setDownloading(false);
  };

  return (
    <Layout title="Reports">
      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 3, borderBottom: '1px solid #E3EAF2' }}>
        <Tab icon={<BarChartIcon />} iconPosition="start" label="Sales Report" />
        <Tab icon={<InventoryIcon />} iconPosition="start" label="Inventory Report" />
        <Tab icon={<TableChartIcon />} iconPosition="start" label="Revenue vs Expenses" />
      </Tabs>

      {/* ── Sales Report ── */}
      {tab === 0 && (
        <Box>
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h6" fontWeight={700} mb={2}>Sales Report Filters</Typography>
              <Grid container spacing={2} alignItems="center">
                <Grid item xs={12} sm={3}>
                  <FormControl fullWidth size="small">
                    <InputLabel>Branch</InputLabel>
                    <Select value={branchId} label="Branch" onChange={(e) => setBranchId(e.target.value as number | '')}>
                      <MenuItem value="">All Branches</MenuItem>
                      {branches.map((b) => <MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}
                    </Select>
                  </FormControl>
                </Grid>
                <Grid item xs={12} sm={3}>
                  <TextField fullWidth size="small" label="From" type="datetime-local" value={start} onChange={(e) => setStart(e.target.value)} InputLabelProps={{ shrink: true }} />
                </Grid>
                <Grid item xs={12} sm={3}>
                  <TextField fullWidth size="small" label="To" type="datetime-local" value={end} onChange={(e) => setEnd(e.target.value)} InputLabelProps={{ shrink: true }} />
                </Grid>
                <Grid item xs={12} sm={3}>
                  <Box display="flex" gap={1}>
                    <Button variant="contained" onClick={() => refetchSales()} disabled={salesLoading} fullWidth>
                      {salesLoading ? <CircularProgress size={18} color="inherit" /> : 'Run Report'}
                    </Button>
                    <Button variant="outlined" size="small" onClick={() => handleSalesExport('csv')} disabled={downloading}>CSV</Button>
                    <Button variant="outlined" size="small" onClick={() => handleSalesExport('pdf')} disabled={downloading}>PDF</Button>
                    <Button variant="outlined" size="small" onClick={() => handleSalesExport('excel')} disabled={downloading} color="success">XLS</Button>
                  </Box>
                </Grid>
              </Grid>
            </CardContent>
          </Card>

          {salesError && <Alert severity="error" sx={{ mb: 2 }}>Failed to load report</Alert>}

          {salesReport && (
            <Grid container spacing={3}>
              <Grid item xs={12} sm={4}>
                <Card><CardContent sx={{ textAlign: 'center' }}>
                  <Typography variant="h3" fontWeight={800} color="primary.main">{salesReport.totalTransactions}</Typography>
                  <Typography color="text.secondary">Total Transactions</Typography>
                </CardContent></Card>
              </Grid>
              <Grid item xs={12} sm={4}>
                <Card><CardContent sx={{ textAlign: 'center' }}>
                  <Typography variant="h3" fontWeight={800} color="success.main">{fmt(salesReport.totalRevenue)}</Typography>
                  <Typography color="text.secondary">Total Revenue</Typography>
                </CardContent></Card>
              </Grid>
              <Grid item xs={12} sm={4}>
                <Card><CardContent sx={{ textAlign: 'center' }}>
                  <Typography variant="h3" fontWeight={800} color="secondary.main">{fmt(salesReport.averageOrderValue)}</Typography>
                  <Typography color="text.secondary">Average Order Value</Typography>
                </CardContent></Card>
              </Grid>

              <Grid item xs={12}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" fontWeight={700} mb={1}>
                      Top Products — {salesReport.branchName} · {salesReport.period}
                    </Typography>
                    <Divider sx={{ mb: 2 }} />
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell sx={{ fontWeight: 700 }}>#</TableCell>
                          <TableCell sx={{ fontWeight: 700 }}>Product</TableCell>
                          <TableCell align="right" sx={{ fontWeight: 700 }}>Qty Sold</TableCell>
                          <TableCell align="right" sx={{ fontWeight: 700 }}>Revenue</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {salesReport.topProducts.map((p, i) => (
                          <TableRow key={p.productId} hover>
                            <TableCell sx={{ fontWeight: 600, color: 'text.secondary' }}>#{i + 1}</TableCell>
                            <TableCell>{p.productName}</TableCell>
                            <TableCell align="right">{p.quantitySold}</TableCell>
                            <TableCell align="right" sx={{ fontWeight: 700 }}>{fmt(p.revenue)}</TableCell>
                          </TableRow>
                        ))}
                        {salesReport.topProducts.length === 0 && (
                          <TableRow><TableCell colSpan={4} align="center" sx={{ py: 3, color: 'text.secondary' }}>No sales in this period</TableCell></TableRow>
                        )}
                      </TableBody>
                    </Table>
                  </CardContent>
                </Card>
              </Grid>
            </Grid>
          )}
        </Box>
      )}

      {/* ── Inventory Report ── */}
      {tab === 1 && (
        <Box>
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Box display="flex" gap={2} alignItems="center" flexWrap="wrap">
                <FormControl size="small" sx={{ minWidth: 220 }}>
                  <InputLabel>Branch</InputLabel>
                  <Select value={invBranchId} label="Branch" onChange={(e) => setInvBranchId(e.target.value as number | '')}>
                    <MenuItem value="">All Branches</MenuItem>
                    {branches.map((b) => <MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}
                  </Select>
                </FormControl>
                <Button variant="outlined" size="small" startIcon={<DownloadIcon />} onClick={() => handleInventoryExport('csv')} disabled={!invBranchId || downloading}>CSV</Button>
                <Button variant="outlined" size="small" onClick={() => handleInventoryExport('pdf')} disabled={!invBranchId || downloading}>PDF</Button>
                <Button variant="outlined" size="small" onClick={() => handleInventoryExport('excel')} disabled={!invBranchId || downloading} color="success">XLS</Button>
                <Box flex={1} />
                <Box display="flex" gap={1.5}>
                  <Chip label={`${inventory.length} items`} />
                  <Chip icon={<WarningAmberIcon />} label={`${inventory.filter((i) => i.lowStock).length} low stock`} color="error" variant="outlined" />
                </Box>
              </Box>
            </CardContent>
          </Card>

          <Card>
            <Table size="small">
              <TableHead>
                <TableRow sx={{ bgcolor: '#F0F4F8' }}>
                  <TableCell sx={{ fontWeight: 700 }}>Product</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>SKU</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Branch</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 700 }}>Qty</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 700 }}>Reorder At</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Status</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Last Updated</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {invLoading ? (
                  <TableRow><TableCell colSpan={7} align="center"><CircularProgress /></TableCell></TableRow>
                ) : inventory.map((item) => (
                  <TableRow key={item.id} hover sx={{ bgcolor: item.lowStock ? '#FFF3E0' : 'inherit' }}>
                    <TableCell>{item.productName}</TableCell>
                    <TableCell sx={{ color: 'text.secondary' }}>{item.productSku ?? '—'}</TableCell>
                    <TableCell>{item.branchName}</TableCell>
                    <TableCell align="right" sx={{ fontWeight: 700, color: item.lowStock ? 'error.main' : 'success.main' }}>{item.quantity}</TableCell>
                    <TableCell align="right">{item.reorderLevel}</TableCell>
                    <TableCell>
                      {item.lowStock ? <Chip icon={<WarningAmberIcon />} label="Low Stock" color="error" size="small" /> : <Chip label="OK" color="success" size="small" />}
                    </TableCell>
                    <TableCell sx={{ color: 'text.secondary', fontSize: 12 }}>{item.lastUpdated ? new Date(item.lastUpdated).toLocaleString() : '—'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Card>
        </Box>
      )}

      {/* ── Revenue vs Expenses ── */}
      {tab === 2 && <RevenueVsExpensesTab />}
    </Layout>
  );
};

const RevenueVsExpensesTab = () => {
  const [branchId, setBranchId] = useState<number | ''>('');
  const [start, setStart] = useState(new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().slice(0, 16));
  const [end, setEnd] = useState(new Date().toISOString().slice(0, 16));
  const { data: branches = [] } = useQuery({ queryKey: ['branches'], queryFn: branchesApi.getActive });
  const { data: salesReport, refetch, isLoading } = useQuery({
    queryKey: ['rev-exp-sales', branchId, start, end],
    queryFn: () => reportsApi.getSalesReport({ branchId: branchId || undefined, start: new Date(start).toISOString(), end: new Date(end).toISOString() }),
    enabled: false,
  });
  const { data: expenses = [] } = useQuery({
    queryKey: ['expenses', branchId],
    queryFn: () => branchId ? expensesApi.getByBranch(branchId) : expensesApi.getAll(),
  });

  const totalRevenue = salesReport?.totalRevenue ?? 0;
  const totalExpenses = expenses.reduce((s, e) => s + e.amount, 0);
  const netProfit = totalRevenue - totalExpenses;

  return (
    <Box>
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box display="flex" gap={2} flexWrap="wrap" alignItems="center">
            <FormControl size="small" sx={{ minWidth: 200 }}>
              <InputLabel>Branch</InputLabel>
              <Select value={branchId} label="Branch" onChange={(e) => setBranchId(e.target.value as number | '')}>
                <MenuItem value="">All Branches</MenuItem>
                {branches.map((b) => <MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField size="small" label="From" type="datetime-local" InputLabelProps={{ shrink: true }} value={start} onChange={(e) => setStart(e.target.value)} />
            <TextField size="small" label="To" type="datetime-local" InputLabelProps={{ shrink: true }} value={end} onChange={(e) => setEnd(e.target.value)} />
            <Button variant="contained" onClick={() => refetch()} disabled={isLoading}>
              {isLoading ? <CircularProgress size={18} color="inherit" /> : 'Analyse'}
            </Button>
          </Box>
        </CardContent>
      </Card>

      <Grid container spacing={3} mb={3}>
        {[
          { label: 'Total Revenue', value: totalRevenue, color: 'success.main' },
          { label: 'Total Expenses', value: totalExpenses, color: 'error.main' },
          { label: 'Net Profit', value: netProfit, color: netProfit >= 0 ? 'primary.main' : 'error.main' },
        ].map((s) => (
          <Grid item xs={12} md={4} key={s.label}>
            <Card><CardContent sx={{ textAlign: 'center' }}>
              <Typography variant="h4" fontWeight={800} color={s.color}>${Number(s.value).toFixed(2)}</Typography>
              <Typography color="text.secondary">{s.label}</Typography>
            </CardContent></Card>
          </Grid>
        ))}
      </Grid>

      <Card>
        <CardContent>
          <Typography variant="h6" fontWeight={700} mb={2}>Revenue vs Expenses — Top Products</Typography>
          {salesReport && salesReport.topProducts.length > 0 ? (
            <ResponsiveContainer width="100%" height={300}>
              <ComposedChart data={salesReport.topProducts.map((p) => ({
                name: p.productName.length > 15 ? p.productName.slice(0, 15) + '…' : p.productName,
                revenue: p.revenue,
                qty: p.quantitySold,
              }))}>
                <CartesianGrid strokeDasharray="3 3" stroke="#E3EAF2" />
                <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                <YAxis yAxisId="left" tick={{ fontSize: 11 }} tickFormatter={(v) => `$${v}`} />
                <YAxis yAxisId="right" orientation="right" tick={{ fontSize: 11 }} />
                <ReTooltip />
                <Legend />
                <Bar yAxisId="left" dataKey="revenue" name="Revenue ($)" fill="#1565C0" radius={[4, 4, 0, 0]} />
                <Line yAxisId="right" type="monotone" dataKey="qty" name="Qty Sold" stroke="#FF8F00" strokeWidth={2} dot />
              </ComposedChart>
            </ResponsiveContainer>
          ) : (
            <Box textAlign="center" py={4} color="text.secondary">
              <Typography>Run the analysis above to see revenue breakdown by product</Typography>
            </Box>
          )}
        </CardContent>
      </Card>
    </Box>
  );
};
