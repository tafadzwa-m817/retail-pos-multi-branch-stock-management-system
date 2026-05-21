import { createContext, useContext, useState, useMemo, type ReactNode } from 'react';
import { createTheme, ThemeProvider } from '@mui/material/styles';

type Mode = 'light' | 'dark';

interface ThemeContextType {
  mode: Mode;
  toggleMode: () => void;
}

const ThemeCtx = createContext<ThemeContextType>({ mode: 'light', toggleMode: () => {} });

export const useThemeMode = () => useContext(ThemeCtx);

export const AppThemeProvider = ({ children }: { children: ReactNode }) => {
  const [mode, setMode] = useState<Mode>(() =>
    (localStorage.getItem('themeMode') as Mode) ?? 'light'
  );

  const toggleMode = () => {
    setMode((prev) => {
      const next = prev === 'light' ? 'dark' : 'light';
      localStorage.setItem('themeMode', next);
      return next;
    });
  };

  const theme = useMemo(() =>
    createTheme({
      palette: {
        mode,
        primary: { main: '#1565C0', light: '#1E88E5', dark: '#0D47A1', contrastText: '#fff' },
        secondary: { main: '#FF8F00', light: '#FFA000', dark: '#E65100' },
        background: {
          default: mode === 'light' ? '#F0F4F8' : '#0A0E1A',
          paper: mode === 'light' ? '#ffffff' : '#111827',
        },
      },
      typography: {
        fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
        h4: { fontWeight: 700 }, h5: { fontWeight: 600 }, h6: { fontWeight: 600 },
      },
      shape: { borderRadius: 10 },
      components: {
        MuiButton: { styleOverrides: { root: { textTransform: 'none', fontWeight: 600, borderRadius: 8 } } },
        MuiCard: { styleOverrides: { root: { borderRadius: 12, boxShadow: '0 2px 12px rgba(0,0,0,0.08)' } } },
        MuiChip: { styleOverrides: { root: { fontWeight: 600 } } },
      },
    }),
    [mode]
  );

  return (
    <ThemeCtx.Provider value={{ mode, toggleMode }}>
      <ThemeProvider theme={theme}>{children}</ThemeProvider>
    </ThemeCtx.Provider>
  );
};
