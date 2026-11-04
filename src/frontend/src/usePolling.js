import { useEffect } from 'react';

/**
 * Calls load() now and then every intervalMs while the page is open (short polling, see the
 * proposal's technology table). Paused while the browser tab is hidden.
 */
export default function usePolling(load, intervalMs) {
  useEffect(() => {
    load();
    const timer = setInterval(() => {
      if (!document.hidden) load();
    }, intervalMs);
    return () => clearInterval(timer);
  }, [load, intervalMs]);
}
