import { useEffect, useRef, useCallback } from 'react';

/**
 * Listens for rapid keyboard input that resembles a barcode scanner.
 * Scanners type characters faster than humans (~10ms apart) and finish with Enter.
 * Human typing is typically >100ms between characters.
 */
export const useBarcodeScanner = (
  onScan: (barcode: string) => void,
  enabled = true,
  minLength = 3,
  maxKeyInterval = 80,
) => {
  const bufferRef = useRef('');
  const lastKeyTimeRef = useRef(0);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const flush = useCallback(() => {
    const code = bufferRef.current.trim();
    if (code.length >= minLength) {
      onScan(code);
    }
    bufferRef.current = '';
  }, [onScan, minLength]);

  useEffect(() => {
    if (!enabled) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      // Ignore if focus is on an input/textarea (user is typing manually)
      const tag = (e.target as HTMLElement)?.tagName?.toLowerCase();
      if (tag === 'input' || tag === 'textarea') return;

      const now = Date.now();
      const gap = now - lastKeyTimeRef.current;
      lastKeyTimeRef.current = now;

      if (gap > maxKeyInterval && bufferRef.current.length > 0) {
        // Gap too long — this is a new scan attempt, discard previous buffer
        bufferRef.current = '';
      }

      if (e.key === 'Enter') {
        if (timerRef.current) clearTimeout(timerRef.current);
        flush();
        e.preventDefault();
        return;
      }

      if (e.key.length === 1) {
        bufferRef.current += e.key;

        // Auto-flush after 200ms of no input (handles scanners without trailing Enter)
        if (timerRef.current) clearTimeout(timerRef.current);
        timerRef.current = setTimeout(flush, 200);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [enabled, maxKeyInterval, flush]);
};
