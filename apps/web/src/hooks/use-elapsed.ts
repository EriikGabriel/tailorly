import { useEffect, useState } from "react";

/** Segundos desde que `running` ficou verdadeiro (ou desde `since`, em ms); zera quando para. */
export function useElapsedSeconds(running: boolean, since?: number | null) {
  const [origin, setOrigin] = useState<number | null>(null);
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    if (!running) {
      setOrigin(null);
      return;
    }
    setOrigin((current) => since ?? current ?? Date.now());
    setNow(Date.now());
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, [running, since]);

  if (!running || origin === null) return 0;
  return Math.max(0, Math.floor((now - origin) / 1000));
}

/** "8 s", "1 min 05 s". */
export function formatElapsed(seconds: number): string {
  if (seconds < 60) return `${seconds} s`;
  const minutes = Math.floor(seconds / 60);
  return `${minutes} min ${String(seconds % 60).padStart(2, "0")} s`;
}
