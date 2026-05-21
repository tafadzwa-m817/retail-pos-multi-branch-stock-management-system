import { Box, Typography, Button, Dialog, DialogTitle, DialogContent, DialogActions, Chip } from '@mui/material';
import PrintIcon from '@mui/icons-material/Print';
import type { ProductResponse } from '../../types';

interface Props {
  products: ProductResponse[];
  open: boolean;
  onClose: () => void;
}

const LABEL_CSS = `
  @media print {
    body > * { display: none !important; }
    #label-sheet { display: block !important; }
    @page { margin: 5mm; size: A4; }
  }
  #label-sheet {
    font-family: 'Courier New', monospace;
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 4mm;
    padding: 4mm;
  }
  .label {
    border: 1px solid #ccc;
    border-radius: 4px;
    padding: 6px;
    text-align: center;
    page-break-inside: avoid;
    min-height: 30mm;
    display: flex;
    flex-direction: column;
    justify-content: center;
  }
  .label .name { font-size: 11px; font-weight: bold; margin-bottom: 4px; }
  .label .sku  { font-size: 9px; color: #666; margin-bottom: 4px; }
  .label .price { font-size: 18px; font-weight: 900; color: #1565C0; }
  .label .barcode { font-size: 8px; color: #999; margin-top: 4px; font-family: 'Courier New'; }
`;

const printLabels = (products: ProductResponse[]) => {
  const labelsHtml = products.map((p) => `
    <div class="label">
      <div class="name">${p.name}</div>
      ${p.sku ? `<div class="sku">${p.sku}</div>` : ''}
      <div class="price">$${Number(p.sellingPrice).toFixed(2)}</div>
      ${p.barcode ? `<div class="barcode">|||  ${p.barcode}  |||</div>` : ''}
    </div>
  `).join('');

  const win = window.open('', '_blank', 'width=800,height=600');
  if (!win) { alert('Allow pop-ups to print labels'); return; }
  win.document.write(`
    <html><head><title>Price Labels</title>
    <style>${LABEL_CSS}</style></head>
    <body>
      <div id="label-sheet">${labelsHtml}</div>
      <div style="margin:16px;text-align:center">
        <button onclick="window.print()" style="padding:8px 24px;font-size:16px">Print Labels</button>
        <button onclick="window.close()" style="padding:8px 24px;font-size:16px;margin-left:8px">Close</button>
      </div>
    </body></html>
  `);
  win.document.close();
};

export const PriceLabels = ({ products, open, onClose }: Props) => (
  <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
    <DialogTitle fontWeight={700}>
      Print Price Labels — {products.length} product{products.length !== 1 ? 's' : ''}
    </DialogTitle>
    <DialogContent>
      <Typography variant="body2" color="text.secondary" mb={2}>
        Preview of labels that will be printed (3 per row on A4):
      </Typography>
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: 'repeat(3, 1fr)',
          gap: 1.5,
          maxHeight: 400,
          overflowY: 'auto',
        }}
      >
        {products.map((p) => (
          <Box
            key={p.id}
            sx={{
              border: '1px solid',
              borderColor: 'divider',
              borderRadius: 1,
              p: 1.5,
              textAlign: 'center',
              minHeight: 90,
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'center',
            }}
          >
            <Typography variant="caption" fontWeight={700} display="block" noWrap>{p.name}</Typography>
            {p.sku && <Typography variant="caption" color="text.secondary" display="block">{p.sku}</Typography>}
            <Typography variant="h6" fontWeight={900} color="primary.main">
              ${Number(p.sellingPrice).toFixed(2)}
            </Typography>
            {p.barcode && (
              <Typography variant="caption" color="text.disabled" display="block" sx={{ fontFamily: 'monospace', fontSize: 9 }}>
                ||| {p.barcode} |||
              </Typography>
            )}
          </Box>
        ))}
      </Box>
    </DialogContent>
    <DialogActions sx={{ px: 3, pb: 2 }}>
      <Button onClick={onClose}>Cancel</Button>
      <Button
        variant="contained"
        startIcon={<PrintIcon />}
        onClick={() => { printLabels(products); onClose(); }}
      >
        Print Labels
      </Button>
    </DialogActions>
  </Dialog>
);
