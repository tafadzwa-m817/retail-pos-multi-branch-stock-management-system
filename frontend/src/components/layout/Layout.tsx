import { Box, Toolbar } from '@mui/material';
import type { ReactNode } from 'react';
import { Sidebar, DRAWER_WIDTH } from './Sidebar';
import { Header } from './Header';

interface Props {
  children: ReactNode;
  title: string;
}

export const Layout = ({ children, title }: Props) => (
  <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
    <Sidebar />
    <Box sx={{ flexGrow: 1, ml: `${DRAWER_WIDTH}px`, display: 'flex', flexDirection: 'column' }}>
      <Header title={title} />
      <Toolbar />
      <Box component="main" sx={{ flexGrow: 1, p: 3 }}>
        {children}
      </Box>
    </Box>
  </Box>
);
