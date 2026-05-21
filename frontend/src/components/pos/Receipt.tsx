import { Box, Button, Divider, Typography } from '@mui/material';
import PrintIcon from '@mui/icons-material/Print';
import { useQuery } from '@tanstack/react-query';
import { storeSettingsApi, type StoreSettings } from '../../api/storeSettings';
import type { SaleResponse } from '../../types';

interface Props {
  sale: SaleResponse;
  showPrintButton?: boolean;
}

const fmt = (n: number) => `$${n.toFixed(2)}`;

const RECEIPT_WIDTH = '80mm';

const line = (left: string, right: string) =>
  `<div style="display:flex;justify-content:space-between;margin:2px 0">
    <span>${left}</span><span>${right}</span>
  </div>`;

const separator = () => `<div style="border-top:1px dashed #000;margin:6px 0"></div>`;

const buildReceiptHTML = (sale: SaleResponse, settings?: StoreSettings): string => {
  const storeName = settings?.storeName ?? 'Retail POS';
  const footer = settings?.receiptFooterText ?? 'Thank you for shopping with us!';
  const date = new Date(sale.createdAt).toLocaleString('en-GB', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });

  const itemsHTML = sale.items.map((item) =>
    `<div style="margin:3px 0">
      <div>${item.productName}</div>
      ${line(`&nbsp;&nbsp;${item.quantity} × ${fmt(item.unitPrice)}`, fmt(item.totalPrice))}
      ${item.discountAmount > 0 ? line('&nbsp;&nbsp;Discount', `-${fmt(item.discountAmount)}`) : ''}
    </div>`
  ).join('');

  return `
    <html><head><title>Receipt #${sale.id}</title>
    <style>
      * { box-sizing: border-box; }
      body {
        font-family: 'Courier New', Courier, monospace;
        font-size: 12px;
        width: ${RECEIPT_WIDTH};
        margin: 0 auto;
        padding: 8px;
        color: #000;
      }
      h2 { text-align: center; margin: 4px 0; font-size: 16px; }
      p { text-align: center; margin: 2px 0; font-size: 11px; }
      .total { font-size: 15px; font-weight: bold; }
      @media print { @page { margin: 0; width: ${RECEIPT_WIDTH}; } }
    </style></head>
    <body>
      <h2>${storeName}</h2>
      <p>july28 Systems</p>
      ${separator()}
      ${line('Branch:', sale.branchName)}
      ${line('Cashier:', sale.cashierName)}
      ${line('Date:', date)}
      ${separator()}
      <div style="font-weight:bold;margin-bottom:4px">Receipt #${sale.id}</div>
      ${separator()}
      ${itemsHTML}
      ${separator()}
      ${line('Subtotal:', fmt(sale.subtotal))}
      ${sale.discountAmount > 0 ? line('Discount:', `-${fmt(sale.discountAmount)}`) : ''}
      ${sale.taxAmount > 0 ? line('Tax:', fmt(sale.taxAmount)) : ''}
      <div class="total">${line('TOTAL:', fmt(sale.totalAmount))}</div>
      ${separator()}
      ${line('Payment:', sale.paymentMethod.replace('_', ' '))}
      ${sale.paymentReference ? line('Ref:', sale.paymentReference) : ''}
      ${sale.customerName ? line('Customer:', sale.customerName) : ''}
      ${sale.customerId ? line('Loyalty pts:', `+${Math.floor(sale.totalAmount)}`) : ''}
      ${separator()}
      <p style="margin-top:8px">${footer}</p>
    </body></html>
  `;
};

export const printReceipt = (sale: SaleResponse, settings?: StoreSettings) => {
  const html = buildReceiptHTML(sale, settings);
  const win = window.open('', '_blank', `width=400,height=600,scrollbars=yes`);
  if (!win) { alert('Allow pop-ups to print receipts'); return; }
  win.document.write(html);
  win.document.close();
  win.focus();
  setTimeout(() => { win.print(); win.close(); }, 300);
};

export const Receipt = ({ sale, showPrintButton = true }: Props) => {
  const { data: settings } = useQuery({
    queryKey: ['store-settings'],
    queryFn: storeSettingsApi.get,
    staleTime: Infinity,
  });

  return <Box>
    {/* Screen preview */}
    <Box
      sx={{
        fontFamily: 'monospace',
        fontSize: 12,
        bgcolor: '#fafafa',
        border: '1px solid #e0e0e0',
        borderRadius: 2,
        p: 2,
        maxWidth: 320,
        mx: 'auto',
      }}
    >
      <Typography variant="subtitle2" fontWeight={800} textAlign="center">{settings?.storeName ?? 'RETAIL POS'}</Typography>
      <Typography variant="caption" display="block" textAlign="center" color="text.secondary">
        july28 Systems
      </Typography>
      <Divider sx={{ my: 1, borderStyle: 'dashed' }} />

      <Box display="flex" justifyContent="space-between" mb={0.25}>
        <Typography variant="caption">Branch</Typography>
        <Typography variant="caption" fontWeight={700}>{sale.branchName}</Typography>
      </Box>
      <Box display="flex" justifyContent="space-between" mb={0.25}>
        <Typography variant="caption">Cashier</Typography>
        <Typography variant="caption">{sale.cashierName}</Typography>
      </Box>
      <Box display="flex" justifyContent="space-between" mb={0.25}>
        <Typography variant="caption">Date</Typography>
        <Typography variant="caption">{new Date(sale.createdAt).toLocaleString()}</Typography>
      </Box>

      <Divider sx={{ my: 1, borderStyle: 'dashed' }} />
      <Typography variant="caption" fontWeight={700} display="block" mb={0.5}>
        Receipt #{sale.id}
      </Typography>

      {sale.items.map((item) => (
        <Box key={item.id} mb={0.5}>
          <Typography variant="caption" display="block">{item.productName}</Typography>
          <Box display="flex" justifyContent="space-between">
            <Typography variant="caption" color="text.secondary">
              &nbsp;&nbsp;{item.quantity} × {fmt(item.unitPrice)}
            </Typography>
            <Typography variant="caption" fontWeight={600}>{fmt(item.totalPrice)}</Typography>
          </Box>
        </Box>
      ))}

      <Divider sx={{ my: 1, borderStyle: 'dashed' }} />
      <Box display="flex" justifyContent="space-between">
        <Typography variant="caption">Subtotal</Typography>
        <Typography variant="caption">{fmt(sale.subtotal)}</Typography>
      </Box>
      {sale.discountAmount > 0 && (
        <Box display="flex" justifyContent="space-between">
          <Typography variant="caption" color="error.main">Discount</Typography>
          <Typography variant="caption" color="error.main">-{fmt(sale.discountAmount)}</Typography>
        </Box>
      )}
      <Box display="flex" justifyContent="space-between" mt={0.5}>
        <Typography variant="caption" fontWeight={800}>TOTAL</Typography>
        <Typography variant="caption" fontWeight={800} color="primary.main">{fmt(sale.totalAmount)}</Typography>
      </Box>

      <Divider sx={{ my: 1, borderStyle: 'dashed' }} />
      <Box display="flex" justifyContent="space-between">
        <Typography variant="caption">Payment</Typography>
        <Typography variant="caption">{sale.paymentMethod.replace('_', ' ')}</Typography>
      </Box>
      {sale.customerName && (
        <>
          <Box display="flex" justifyContent="space-between">
            <Typography variant="caption">Customer</Typography>
            <Typography variant="caption">{sale.customerName}</Typography>
          </Box>
          <Box display="flex" justifyContent="space-between">
            <Typography variant="caption" color="success.main">Loyalty pts</Typography>
            <Typography variant="caption" color="success.main">+{Math.floor(sale.totalAmount)}</Typography>
          </Box>
        </>
      )}

      <Divider sx={{ my: 1, borderStyle: 'dashed' }} />
      <Typography variant="caption" display="block" textAlign="center">
        {settings?.receiptFooterText ?? 'Thank you for shopping!'}
      </Typography>
    </Box>

    {showPrintButton && (
      <Box display="flex" justifyContent="center" mt={2}>
        <Button variant="outlined" startIcon={<PrintIcon />} onClick={() => printReceipt(sale, settings)}>
          Print Receipt
        </Button>
      </Box>
    )}
  </Box>;
};
