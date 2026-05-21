import { useEffect } from 'react';
import type { PaymentMethod } from '../types';

interface Handlers {
  onPaymentMethodChange: (method: PaymentMethod) => void;
  onCharge: () => void;
  onClearCart: () => void;
  onFocusSearch: () => void;
}

const KEY_MAP: Record<string, PaymentMethod> = {
  F1: 'CASH',
  F2: 'CARD',
  F3: 'MOBILE_MONEY',
  F4: 'BANK_TRANSFER',
  F5: 'CREDIT',
};

/**
 * POS keyboard shortcuts:
 *   F1-F5       → Select payment method
 *   Ctrl+Enter  → Charge / confirm payment
 *   Ctrl+Del    → Clear cart
 *   Ctrl+F      → Focus product search
 */
export const usePOSKeyboardShortcuts = (handlers: Handlers) => {
  useEffect(() => {
    const handleKey = (e: KeyboardEvent) => {
      // Don't fire if typing in an input
      const tag = (e.target as HTMLElement)?.tagName?.toLowerCase();
      if (tag === 'textarea') return;

      if (KEY_MAP[e.key]) {
        e.preventDefault();
        handlers.onPaymentMethodChange(KEY_MAP[e.key]);
        return;
      }

      if (e.ctrlKey && e.key === 'Enter') {
        e.preventDefault();
        handlers.onCharge();
        return;
      }

      if (e.ctrlKey && e.key === 'Delete') {
        e.preventDefault();
        handlers.onClearCart();
        return;
      }

      if (e.ctrlKey && e.key === 'f') {
        e.preventDefault();
        handlers.onFocusSearch();
      }
    };

    window.addEventListener('keydown', handleKey);
    return () => window.removeEventListener('keydown', handleKey);
  }, [handlers]);
};
