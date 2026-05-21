import {
  Dialog, DialogTitle, DialogContent, Table, TableHead, TableRow,
  TableCell, TableBody, Typography, Chip, Box, Divider,
} from '@mui/material';
import KeyboardIcon from '@mui/icons-material/Keyboard';
import { useEffect, useState } from 'react';

const shortcuts = [
  { group: 'Payment Methods', keys: [
    { key: 'F1', description: 'Select Cash payment' },
    { key: 'F2', description: 'Select Card payment' },
    { key: 'F3', description: 'Select Mobile Money payment' },
    { key: 'F4', description: 'Select Bank Transfer payment' },
    { key: 'F5', description: 'Select Credit payment' },
  ]},
  { group: 'Cart Actions', keys: [
    { key: 'Ctrl + Enter', description: 'Charge / open payment dialog' },
    { key: 'Ctrl + Del', description: 'Clear entire cart' },
    { key: 'Ctrl + F', description: 'Focus product search bar' },
  ]},
  { group: 'Barcode Scanner', keys: [
    { key: 'Any barcode', description: 'Scan product → auto-add to cart' },
    { key: 'CUS-{id}', description: 'Scan customer QR code → auto-select customer' },
  ]},
  { group: 'Help', keys: [
    { key: '?', description: 'Show this keyboard shortcut guide' },
  ]},
];

interface Props {
  open: boolean;
  onClose: () => void;
}

export const ShortcutHelpModal = ({ open, onClose }: Props) => (
  <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
    <DialogTitle>
      <Box display="flex" alignItems="center" gap={1}>
        <KeyboardIcon color="primary" />
        <Typography variant="h6" fontWeight={700}>POS Keyboard Shortcuts</Typography>
      </Box>
    </DialogTitle>
    <DialogContent sx={{ pt: 0 }}>
      {shortcuts.map((group, gi) => (
        <Box key={gi} mb={2}>
          <Typography variant="caption" fontWeight={700} color="text.secondary" sx={{ letterSpacing: 1 }}>
            {group.group.toUpperCase()}
          </Typography>
          <Table size="small" sx={{ mt: 0.5 }}>
            <TableBody>
              {group.keys.map((s, i) => (
                <TableRow key={i} hover>
                  <TableCell width={160}>
                    <Chip
                      label={s.key}
                      size="small"
                      sx={{ fontFamily: 'monospace', fontWeight: 700, bgcolor: 'action.hover' }}
                    />
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">{s.description}</Typography>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          {gi < shortcuts.length - 1 && <Divider sx={{ mt: 1.5 }} />}
        </Box>
      ))}
      <Typography variant="caption" color="text.secondary" display="block" textAlign="center" mt={1}>
        Press <strong>?</strong> anywhere in the POS to show this guide
      </Typography>
    </DialogContent>
  </Dialog>
);

/** Global hook — press ? to open shortcut help */
export const useShortcutHelp = () => {
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      const tag = (e.target as HTMLElement)?.tagName?.toLowerCase();
      if (tag === 'input' || tag === 'textarea') return;
      if (e.key === '?') setOpen(true);
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, []);

  return { open, close: () => setOpen(false) };
};
