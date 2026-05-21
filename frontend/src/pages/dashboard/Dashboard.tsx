import {
  Grid, Card, CardContent, Typography, Box, Table, TableHead,
  TableRow, TableCell, TableBody, Chip, CircularProgress, Alert,
  TableContainer, Paper, ButtonGroup, Button,
} from '@mui/material';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import ReceiptIcon from '@mui/icons-material/Receipt';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import StoreIcon from '@mui/icons-material/Store';
import ShoppingBagIcon from '@mui/icons-material/ShoppingBag';
import PeopleIcon from '@mui/icons-material/People';
import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  ResponsiveContainer, AreaChart, Area, XAxis, YAxis,
  CartesianGrid, Tooltip as ReTooltip, BarChart, Bar, Legend,
} from 'recharts';
import { Layout } from '../../components/layout/Layout';
import { StatCard } from '../../components/common/StatCard';
import { dashboardApi } from '../../api/dashboard';
import { reportsApi } from '../../api/reports';
import { useWebSocket } from '../../hooks/useWebSocket';
import type { DashboardResponse } from '../../types';

const fmt = (n: number) =>
  `$${n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

export const Dashboard = () => {
  const queryClient = useQueryClient();
  const [chartDays, setChartDays] = useState(7);

  const { data, isLoading, error } = useQuery({
    queryKey: ['dashboard'],
    queryFn: dashboardApi.get,
    refetchInterval: 60_000,
  });

  const { data: dailySummary = [] } = useQuery({
    queryKey: ['daily-summary', chartDays],
    queryFn: () => reportsApi.getDailySummary(chartDays),
  });

  // Live WebSocket updates — silently ignored if backend WebSocket unavailable
  useWebSocket<DashboardResponse>('/topic/dashboard', (live) => {
    queryClient.setQueryData(['dashboard'], live);
  });

  return (
    <Layout title="Dashboard">
      {isLoading && (
        <Box display="flex" justifyContent="center" mt={8}>
          <CircularProgress />
        </Box>
      )}

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          Failed to load dashboard data.
        </Alert>
      )}

      {data && (
        <Box>
          {/* Stat Cards */}
          <Grid container spacing={3} mb={3}>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Today's Sales"
                value={data.todaySalesCount}
                subtitle="transactions"
                icon={<ReceiptIcon />}
                color="#1565C0"
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Today's Revenue"
                value={fmt(data.todayRevenue)}
                subtitle={`Avg ${fmt(data.todayAverageOrderValue)} / order`}
                icon={<TrendingUpIcon />}
                color="#2E7D32"
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Low Stock Items"
                value={data.lowStockItemCount}
                subtitle="need restocking"
                icon={<WarningAmberIcon />}
                color={data.lowStockItemCount > 0 ? '#C62828' : '#2E7D32'}
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Active Branches"
                value={data.totalActiveBranches}
                subtitle={`${data.totalActiveUsers} staff · ${data.totalActiveCustomers} customers`}
                icon={<StoreIcon />}
                color="#6A1B9A"
              />
            </Grid>
          </Grid>

          {/* Revenue Chart */}
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                <Box display="flex" alignItems="center" gap={1}>
                  <TrendingUpIcon color="success" fontSize="small" />
                  <Typography variant="h6">Revenue Trend</Typography>
                </Box>
                <ButtonGroup size="small" variant="outlined">
                  {[7, 14, 30].map((d) => (
                    <Button key={d} variant={chartDays === d ? 'contained' : 'outlined'} onClick={() => setChartDays(d)}>
                      {d}d
                    </Button>
                  ))}
                </ButtonGroup>
              </Box>
              <ResponsiveContainer width="100%" height={220}>
                <AreaChart data={dailySummary} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
                  <defs>
                    <linearGradient id="revGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#1565C0" stopOpacity={0.2} />
                      <stop offset="95%" stopColor="#1565C0" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#E3EAF2" />
                  <XAxis dataKey="date" tick={{ fontSize: 11 }} tickFormatter={(d) => d.slice(5)} />
                  <YAxis tick={{ fontSize: 11 }} tickFormatter={(v) => `$${v}`} />
                  <ReTooltip formatter={(v) => v != null ? [`$${Number(v).toFixed(2)}`, 'Revenue'] : ['-', 'Revenue']} labelFormatter={(l) => `Date: ${l}`} />
                  <Area type="monotone" dataKey="revenue" stroke="#1565C0" strokeWidth={2} fill="url(#revGrad)" />
                </AreaChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>

          {/* Sales Count Bar Chart */}
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Box display="flex" alignItems="center" gap={1} mb={2}>
                <ReceiptIcon color="primary" fontSize="small" />
                <Typography variant="h6">Daily Transactions ({chartDays} days)</Typography>
              </Box>
              <ResponsiveContainer width="100%" height={160}>
                <BarChart data={dailySummary} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#E3EAF2" />
                  <XAxis dataKey="date" tick={{ fontSize: 11 }} tickFormatter={(d) => d.slice(5)} />
                  <YAxis tick={{ fontSize: 11 }} allowDecimals={false} />
                  <ReTooltip formatter={(v) => [v ?? 0, 'Transactions']} />
                  <Bar dataKey="salesCount" fill="#1E88E5" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>

          <Grid container spacing={3}>
            {/* Branch Performance */}
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent sx={{ pb: '16px !important' }}>
                  <Box display="flex" alignItems="center" gap={1} mb={2}>
                    <StoreIcon color="primary" fontSize="small" />
                    <Typography variant="h6">Branch Performance — Today</Typography>
                  </Box>
                  <TableContainer>
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell sx={{ fontWeight: 700 }}>Branch</TableCell>
                          <TableCell align="right" sx={{ fontWeight: 700 }}>Sales</TableCell>
                          <TableCell align="right" sx={{ fontWeight: 700 }}>Revenue</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {data.branchPerformance.map((b) => (
                          <TableRow key={b.branchId} hover>
                            <TableCell>{b.branchName}</TableCell>
                            <TableCell align="right">
                              <Chip label={b.salesCount} size="small" color="primary" variant="outlined" />
                            </TableCell>
                            <TableCell align="right" sx={{ fontWeight: 600 }}>
                              {fmt(b.revenue)}
                            </TableCell>
                          </TableRow>
                        ))}
                        {data.branchPerformance.length === 0 && (
                          <TableRow>
                            <TableCell colSpan={3} align="center" sx={{ color: 'text.secondary', py: 3 }}>
                              No sales today yet
                            </TableCell>
                          </TableRow>
                        )}
                      </TableBody>
                    </Table>
                  </TableContainer>
                </CardContent>
              </Card>
            </Grid>

            {/* Top Products */}
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent sx={{ pb: '16px !important' }}>
                  <Box display="flex" alignItems="center" gap={1} mb={2}>
                    <ShoppingBagIcon color="secondary" fontSize="small" />
                    <Typography variant="h6">Top Products — Today</Typography>
                  </Box>
                  <TableContainer>
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell sx={{ fontWeight: 700 }}>#</TableCell>
                          <TableCell sx={{ fontWeight: 700 }}>Product</TableCell>
                          <TableCell align="right" sx={{ fontWeight: 700 }}>Qty</TableCell>
                          <TableCell align="right" sx={{ fontWeight: 700 }}>Revenue</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {data.topProductsToday.map((p, i) => (
                          <TableRow key={p.productId} hover>
                            <TableCell sx={{ color: 'text.secondary', fontWeight: 600 }}>#{i + 1}</TableCell>
                            <TableCell>{p.productName}</TableCell>
                            <TableCell align="right">{p.quantitySold}</TableCell>
                            <TableCell align="right" sx={{ fontWeight: 600 }}>
                              {fmt(p.revenue)}
                            </TableCell>
                          </TableRow>
                        ))}
                        {data.topProductsToday.length === 0 && (
                          <TableRow>
                            <TableCell colSpan={4} align="center" sx={{ color: 'text.secondary', py: 3 }}>
                              No sales today yet
                            </TableCell>
                          </TableRow>
                        )}
                      </TableBody>
                    </Table>
                  </TableContainer>
                </CardContent>
              </Card>
            </Grid>

            {/* Summary strip */}
            <Grid item xs={12}>
              <Paper sx={{ p: 2.5, display: 'flex', gap: 4, flexWrap: 'wrap', bgcolor: '#EFF3FB' }}>
                {[
                  { label: 'Total Products', value: data.totalActiveProducts, icon: <ShoppingBagIcon fontSize="small" /> },
                  { label: 'Total Customers', value: data.totalActiveCustomers, icon: <PeopleIcon fontSize="small" /> },
                  { label: 'Total Branches', value: data.totalActiveBranches, icon: <StoreIcon fontSize="small" /> },
                  { label: 'Total Staff', value: data.totalActiveUsers, icon: <PeopleIcon fontSize="small" /> },
                ].map((s) => (
                  <Box key={s.label} display="flex" alignItems="center" gap={1}>
                    <Box sx={{ color: 'primary.main' }}>{s.icon}</Box>
                    <Box>
                      <Typography variant="caption" color="text.secondary">{s.label}</Typography>
                      <Typography variant="subtitle1" fontWeight={700}>{s.value}</Typography>
                    </Box>
                  </Box>
                ))}
              </Paper>
            </Grid>
          </Grid>
        </Box>
      )}
    </Layout>
  );
};
