import {
  Drawer, List, ListItemButton, ListItemIcon, ListItemText,
  Toolbar, Typography, Box, Divider, Chip,
} from '@mui/material';
import DashboardIcon from '@mui/icons-material/Dashboard';
import PointOfSaleIcon from '@mui/icons-material/PointOfSale';
import InventoryIcon from '@mui/icons-material/Inventory';
import SwapHorizIcon from '@mui/icons-material/SwapHoriz';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import ReceiptIcon from '@mui/icons-material/Receipt';
import PeopleIcon from '@mui/icons-material/People';
import CategoryIcon from '@mui/icons-material/Category';
import LocalOfferIcon from '@mui/icons-material/LocalOffer';
import BarChartIcon from '@mui/icons-material/BarChart';
import SettingsIcon from '@mui/icons-material/Settings';
import LockOpenIcon from '@mui/icons-material/LockOpen';
import AssignmentReturnIcon from '@mui/icons-material/AssignmentReturn';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import SecurityIcon from '@mui/icons-material/Security';
import StorageIcon from '@mui/icons-material/Storage';
import TrackChangesIcon from '@mui/icons-material/TrackChanges';
import DeleteSweepIcon from '@mui/icons-material/DeleteSweep';
import CreditCardIcon from '@mui/icons-material/CreditCard';
import PersonIcon from '@mui/icons-material/Person';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export const DRAWER_WIDTH = 248;

const navGroups = [
  {
    label: 'Main',
    items: [
      { label: 'Dashboard', icon: <DashboardIcon />, path: '/dashboard' },
      { label: 'New Sale (POS)', icon: <PointOfSaleIcon />, path: '/pos' },
    ],
  },
  {
    label: 'Stock',
    items: [
      { label: 'Inventory', icon: <InventoryIcon />, path: '/inventory' },
      { label: 'Stock Transfers', icon: <SwapHorizIcon />, path: '/transfers' },
      { label: 'Purchase Orders', icon: <ShoppingCartIcon />, path: '/purchase-orders' },
    ],
  },
  {
    label: 'Sales',
    items: [
      { label: 'Sales History', icon: <ReceiptIcon />, path: '/sales' },
      { label: 'Cash Shifts', icon: <LockOpenIcon />, path: '/shifts' },
      { label: 'Returns', icon: <AssignmentReturnIcon />, path: '/returns' },
      { label: 'Credit Accounts', icon: <CreditCardIcon />, path: '/credit-accounts' },
      { label: 'Customers', icon: <PeopleIcon />, path: '/customers' },
      { label: 'Promotions', icon: <LocalOfferIcon />, path: '/promotions' },
    ],
  },
  {
    label: 'Catalogue',
    items: [
      { label: 'Products', icon: <CategoryIcon />, path: '/products' },
      { label: 'Supplier Catalog', icon: <StorageIcon />, path: '/supplier-catalog' },
    ],
  },
  {
    label: 'Analytics',
    items: [
      { label: 'Reports', icon: <BarChartIcon />, path: '/reports' },
      { label: 'Sales Targets', icon: <TrackChangesIcon />, path: '/targets' },
      { label: 'Expenses', icon: <AccountBalanceWalletIcon />, path: '/expenses' },
      { label: 'Wastage', icon: <DeleteSweepIcon />, path: '/wastage' },
    ],
  },
  {
    label: 'Admin',
    items: [
      { label: 'Audit Logs', icon: <SecurityIcon />, path: '/audit-logs' },
      { label: 'Settings', icon: <SettingsIcon />, path: '/settings' },
    ],
  },
];

export const Sidebar = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();

  return (
    <Drawer
      variant="permanent"
      sx={{
        width: DRAWER_WIDTH,
        flexShrink: 0,
        '& .MuiDrawer-paper': {
          width: DRAWER_WIDTH,
          boxSizing: 'border-box',
          bgcolor: '#0D47A1',
          color: '#fff',
          borderRight: 'none',
        },
      }}
    >
      <Toolbar sx={{ px: 2.5, py: 2 }}>
        <Box>
          <Typography variant="h6" fontWeight={800} color="#fff" lineHeight={1.1}>
            Retail POS
          </Typography>
          <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.6)' }}>
            july28 Systems
          </Typography>
        </Box>
      </Toolbar>

      <Divider sx={{ borderColor: 'rgba(255,255,255,0.12)' }} />

      <Box sx={{ px: 2, py: 1.5 }}>
        <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.5)', fontWeight: 600, letterSpacing: 1 }}>
          {user?.fullName}
        </Typography>
        <Box mt={0.5}>
          <Chip
            label={user?.role?.replace('_', ' ')}
            size="small"
            sx={{ bgcolor: 'rgba(255,255,255,0.15)', color: '#fff', fontSize: 10, fontWeight: 700 }}
          />
        </Box>
      </Box>

      <Divider sx={{ borderColor: 'rgba(255,255,255,0.12)', mb: 1 }} />

      <Box sx={{ overflowY: 'auto', flex: 1 }}>
        {navGroups.map((group) => (
          <Box key={group.label} sx={{ mb: 1 }}>
            <Typography
              variant="caption"
              sx={{ px: 2.5, py: 0.5, color: 'rgba(255,255,255,0.4)', fontWeight: 700, letterSpacing: 1, display: 'block' }}
            >
              {group.label.toUpperCase()}
            </Typography>
            <List dense disablePadding>
              {group.items.map((item) => {
                const active = location.pathname === item.path;
                return (
                  <ListItemButton
                    key={item.path}
                    onClick={() => navigate(item.path)}
                    sx={{
                      mx: 1,
                      mb: 0.25,
                      borderRadius: 2,
                      bgcolor: active ? 'rgba(255,255,255,0.15)' : 'transparent',
                      '&:hover': { bgcolor: 'rgba(255,255,255,0.1)' },
                    }}
                  >
                    <ListItemIcon sx={{ minWidth: 36, color: active ? '#fff' : 'rgba(255,255,255,0.6)' }}>
                      {item.icon}
                    </ListItemIcon>
                    <ListItemText
                      primary={item.label}
                      primaryTypographyProps={{
                        fontSize: 13.5,
                        fontWeight: active ? 700 : 400,
                        color: active ? '#fff' : 'rgba(255,255,255,0.75)',
                      }}
                    />
                  </ListItemButton>
                );
              })}
            </List>
          </Box>
        ))}
      </Box>
    </Drawer>
  );
};
