import {
  Box, Card, Typography, Chip, TextField, FormControl, InputLabel,
  Select, MenuItem, InputAdornment,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import SearchIcon from '@mui/icons-material/Search';
import SecurityIcon from '@mui/icons-material/Security';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Layout } from '../../components/layout/Layout';
import { auditLogsApi } from '../../api/auditLogs';
import type { AuditLogEntry } from '../../types';

const actionColor = (action: string): 'success' | 'error' | 'warning' | 'info' | 'default' => {
  if (['DELETE', 'VOID', 'CANCEL'].includes(action)) return 'error';
  if (['CREATE', 'RECEIVE'].includes(action)) return 'success';
  if (['UPDATE', 'ADJUST', 'APPROVE', 'COMPLETE'].includes(action)) return 'warning';
  if (['LOGIN', 'LOGOUT'].includes(action)) return 'info';
  return 'default';
};

const ENTITY_TYPES = ['AppUser', 'Sale', 'StockTransfer', 'PurchaseOrder', 'Inventory', 'ProductReturn'];

export const AuditLogs = () => {
  const [page, setPage] = useState(0);
  const [entityType, setEntityType] = useState('');
  const [userEmail, setUserEmail] = useState('');

  const { data: logsPage, isLoading } = useQuery({
    queryKey: ['audit-logs', page, entityType, userEmail],
    queryFn: () => {
      if (userEmail.length >= 3) return auditLogsApi.getByUser(userEmail, page);
      if (entityType) return auditLogsApi.getByEntityType(entityType, page);
      return auditLogsApi.getAll(page);
    },
  });

  const rows = logsPage?.content ?? [];
  const totalRows = logsPage?.totalElements ?? 0;

  const cols: GridColDef<AuditLogEntry>[] = [
    { field: 'id', headerName: '#', width: 70 },
    { field: 'createdAt', headerName: 'Time', flex: 1, minWidth: 160, renderCell: ({ value }) => new Date(value).toLocaleString() },
    {
      field: 'action', headerName: 'Action', width: 120,
      renderCell: ({ value }) => <Chip label={value} color={actionColor(value)} size="small" sx={{ fontWeight: 700 }} />,
    },
    { field: 'entityType', headerName: 'Entity', width: 140, renderCell: ({ value }) => <Chip label={value} size="small" variant="outlined" /> },
    { field: 'entityId', headerName: 'Entity ID', width: 90, renderCell: ({ value }) => value ? `#${value}` : '—' },
    { field: 'performedBy', headerName: 'Performed By', flex: 1, minWidth: 200 },
    {
      field: 'details', headerName: 'Details', flex: 2, minWidth: 240,
      renderCell: ({ value }) => <Typography variant="caption" color="text.secondary" noWrap title={value}>{value ?? '—'}</Typography>,
    },
  ];

  return (
    <Layout title="Audit Logs">
      <Box display="flex" gap={2} mb={2} alignItems="center" flexWrap="wrap">
        <Box display="flex" alignItems="center" gap={1} color="text.secondary">
          <SecurityIcon fontSize="small" />
          <Typography variant="body2">Complete audit trail of all system changes</Typography>
        </Box>
        <Box flex={1} />

        <FormControl size="small" sx={{ minWidth: 180 }}>
          <InputLabel>Entity Type</InputLabel>
          <Select value={entityType} label="Entity Type" onChange={(e) => { setEntityType(e.target.value); setPage(0); }}>
            <MenuItem value="">All Types</MenuItem>
            {ENTITY_TYPES.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
          </Select>
        </FormControl>

        <TextField
          size="small"
          placeholder="Filter by user email…"
          value={userEmail}
          onChange={(e) => { setUserEmail(e.target.value); setPage(0); }}
          sx={{ minWidth: 240 }}
          InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" color="action" /></InputAdornment> }}
        />
      </Box>

      <Card>
        <DataGrid
          rows={rows}
          columns={cols}
          loading={isLoading}
          rowCount={totalRows}
          paginationMode="server"
          paginationModel={{ page, pageSize: 20 }}
          onPaginationModelChange={(m) => setPage(m.page)}
          pageSizeOptions={[20]}
          autoHeight
          disableRowSelectionOnClick
          sx={{ border: 0 }}
        />
      </Card>
    </Layout>
  );
};
