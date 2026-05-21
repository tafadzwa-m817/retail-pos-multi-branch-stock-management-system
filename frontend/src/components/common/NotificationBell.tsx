import {
  Badge, IconButton, Popover, Box, Typography, List, ListItem,
  ListItemText, Divider, Chip, Button, CircularProgress,
} from '@mui/material';
import NotificationsIcon from '@mui/icons-material/Notifications';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import HistoryIcon from '@mui/icons-material/History';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { inventoryApi } from '../../api/inventory';
import { auditLogsApi } from '../../api/auditLogs';

const actionColor = (action: string) => {
  if (['DELETE', 'VOID', 'CANCEL'].includes(action)) return 'error';
  if (['CREATE', 'LOGIN'].includes(action)) return 'success';
  if (['UPDATE', 'ADJUST'].includes(action)) return 'warning';
  return 'default';
};

export const NotificationBell = () => {
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);
  const navigate = useNavigate();

  const { data: lowStock = [], isLoading: loadingStock } = useQuery({
    queryKey: ['low-stock-notifications'],
    queryFn: () => inventoryApi.getLowStock(),
    refetchInterval: 60_000,
  });

  const { data: recentLogs, isLoading: loadingLogs } = useQuery({
    queryKey: ['recent-audit-logs'],
    queryFn: () => auditLogsApi.getAll(0, 5),
    refetchInterval: 30_000,
  });

  const badgeCount = lowStock.length;

  return (
    <>
      <IconButton onClick={(e) => setAnchor(e.currentTarget)} sx={{ mr: 1 }}>
        <Badge badgeContent={badgeCount > 0 ? badgeCount : undefined} color="error" max={99}>
          <NotificationsIcon fontSize="small" />
        </Badge>
      </IconButton>

      <Popover
        open={Boolean(anchor)}
        anchorEl={anchor}
        onClose={() => setAnchor(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
        PaperProps={{ sx: { width: 360, maxHeight: 520, overflow: 'hidden', display: 'flex', flexDirection: 'column' } }}
      >
        <Box sx={{ px: 2, py: 1.5, borderBottom: '1px solid', borderColor: 'divider', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="subtitle1" fontWeight={700}>Notifications</Typography>
          {badgeCount > 0 && <Chip label={`${badgeCount} alerts`} color="error" size="small" />}
        </Box>

        <Box sx={{ overflowY: 'auto', flex: 1 }}>
          {/* Low Stock Section */}
          <Box sx={{ px: 2, pt: 1.5 }}>
            <Box display="flex" alignItems="center" gap={0.5} mb={1}>
              <WarningAmberIcon fontSize="small" color="error" />
              <Typography variant="caption" fontWeight={700} color="error.main">
                LOW STOCK ALERTS
              </Typography>
            </Box>
            {loadingStock ? (
              <Box display="flex" justifyContent="center" p={2}><CircularProgress size={20} /></Box>
            ) : lowStock.length === 0 ? (
              <Typography variant="caption" color="text.secondary">All stock levels are healthy</Typography>
            ) : (
              <List dense disablePadding>
                {lowStock.slice(0, 5).map((item) => (
                  <ListItem key={item.id} disablePadding sx={{ py: 0.25 }}>
                    <ListItemText
                      primary={<Typography variant="body2" fontWeight={600}>{item.productName}</Typography>}
                      secondary={
                        <Typography variant="caption" color="error.main">
                          {item.branchName} — {item.quantity} left (reorder at {item.reorderLevel})
                        </Typography>
                      }
                    />
                  </ListItem>
                ))}
                {lowStock.length > 5 && (
                  <Typography variant="caption" color="text.secondary">+{lowStock.length - 5} more</Typography>
                )}
              </List>
            )}
          </Box>

          <Divider sx={{ my: 1.5 }} />

          {/* Recent Activity */}
          <Box sx={{ px: 2, pb: 1 }}>
            <Box display="flex" alignItems="center" gap={0.5} mb={1}>
              <HistoryIcon fontSize="small" color="action" />
              <Typography variant="caption" fontWeight={700} color="text.secondary">
                RECENT ACTIVITY
              </Typography>
            </Box>
            {loadingLogs ? (
              <Box display="flex" justifyContent="center" p={2}><CircularProgress size={20} /></Box>
            ) : (
              <List dense disablePadding>
                {(recentLogs?.content ?? []).map((log) => (
                  <ListItem key={log.id} disablePadding sx={{ py: 0.25, alignItems: 'flex-start' }}>
                    <ListItemText
                      primary={
                        <Box display="flex" alignItems="center" gap={0.5}>
                          <Chip label={log.action} color={actionColor(log.action) as 'error' | 'success' | 'warning' | 'default'} size="small" sx={{ fontSize: 10, height: 18 }} />
                          <Typography variant="caption" fontWeight={600}>{log.entityType}</Typography>
                          {log.entityId && <Typography variant="caption" color="text.secondary">#{log.entityId}</Typography>}
                        </Box>
                      }
                      secondary={
                        <Typography variant="caption" color="text.secondary">
                          {log.performedBy} · {new Date(log.createdAt).toLocaleTimeString()}
                        </Typography>
                      }
                    />
                  </ListItem>
                ))}
              </List>
            )}
          </Box>
        </Box>

        <Divider />
        <Box sx={{ p: 1, display: 'flex', gap: 1 }}>
          <Button size="small" fullWidth onClick={() => { navigate('/inventory?lowStock=true'); setAnchor(null); }}>
            View Low Stock
          </Button>
          <Button size="small" fullWidth onClick={() => { navigate('/audit-logs'); setAnchor(null); }}>
            All Activity
          </Button>
        </Box>
      </Popover>
    </>
  );
};
