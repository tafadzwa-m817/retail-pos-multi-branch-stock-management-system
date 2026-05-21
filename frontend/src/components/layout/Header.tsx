import { AppBar, Toolbar, Typography, Box, IconButton, Avatar, Menu, MenuItem, Tooltip } from '@mui/material';
import LogoutIcon from '@mui/icons-material/Logout';
import DarkModeIcon from '@mui/icons-material/DarkMode';
import LightModeIcon from '@mui/icons-material/LightMode';
import SettingsIcon from '@mui/icons-material/Settings';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useThemeMode } from '../../context/ThemeContext';
import { NotificationBell } from '../common/NotificationBell';
import { DRAWER_WIDTH } from './Sidebar';

interface Props {
  title: string;
}

export const Header = ({ title }: Props) => {
  const { user, logout } = useAuth();
  const { mode, toggleMode } = useThemeMode();
  const navigate = useNavigate();
  const [anchor, setAnchor] = useState<null | HTMLElement>(null);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const initials = user?.fullName
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2) ?? 'U';

  return (
    <AppBar
      position="fixed"
      elevation={0}
      sx={{
        width: `calc(100% - ${DRAWER_WIDTH}px)`,
        ml: `${DRAWER_WIDTH}px`,
        bgcolor: 'background.paper',
        borderBottom: '1px solid',
        borderColor: 'divider',
        color: 'text.primary',
      }}
    >
      <Toolbar sx={{ px: 3 }}>
        <Typography variant="h6" fontWeight={700} sx={{ flexGrow: 1 }}>
          {title}
        </Typography>

        {/* Notification bell */}
        <NotificationBell />

        {/* Dark mode toggle */}
        <Tooltip title={mode === 'light' ? 'Switch to dark mode' : 'Switch to light mode'}>
          <IconButton onClick={toggleMode} sx={{ mr: 1 }}>
            {mode === 'light' ? <DarkModeIcon fontSize="small" /> : <LightModeIcon fontSize="small" />}
          </IconButton>
        </Tooltip>

        {/* Settings shortcut */}
        <Tooltip title="Settings">
          <IconButton onClick={() => navigate('/settings')} sx={{ mr: 1 }}>
            <SettingsIcon fontSize="small" />
          </IconButton>
        </Tooltip>

        {/* User menu */}
        <Tooltip title={user?.email ?? ''}>
          <IconButton onClick={(e) => setAnchor(e.currentTarget)} sx={{ p: 0.5 }}>
            <Avatar sx={{ bgcolor: '#1565C0', width: 36, height: 36, fontSize: 14, fontWeight: 700 }}>
              {initials}
            </Avatar>
          </IconButton>
        </Tooltip>

        <Menu anchorEl={anchor} open={Boolean(anchor)} onClose={() => setAnchor(null)}>
          <Box sx={{ px: 2, py: 1, minWidth: 180 }}>
            <Typography variant="subtitle2" fontWeight={700}>{user?.fullName}</Typography>
            <Typography variant="caption" color="text.secondary">{user?.email}</Typography>
          </Box>
          <MenuItem onClick={() => { setAnchor(null); navigate('/profile'); }}>
            <Avatar sx={{ width: 18, height: 18, mr: 1, bgcolor: 'primary.main', fontSize: 10 }}>{initials[0]}</Avatar>
            My Profile
          </MenuItem>
          <MenuItem onClick={() => { setAnchor(null); navigate('/settings'); }}>
            <SettingsIcon fontSize="small" sx={{ mr: 1 }} />
            Settings
          </MenuItem>
          <MenuItem onClick={handleLogout} sx={{ color: 'error.main' }}>
            <LogoutIcon fontSize="small" sx={{ mr: 1 }} />
            Logout
          </MenuItem>
        </Menu>
      </Toolbar>
    </AppBar>
  );
};
