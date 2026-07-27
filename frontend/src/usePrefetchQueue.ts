import { useCallback, useEffect, useRef, useState } from "react";
import { Card, fetchNextCard } from "./api";

const BUFFER_SIZE = 3;
const REFILL_THRESHOLD = 2;

export function usePrefetchQueue(language: string) {
  const [queue, setQueue] = useState<Card[]>([]);
  const [error, setError] = useState<string | null>(null);
  const queueRef = useRef<Card[]>([]);
  const fillingRef = useRef(false);

  const setQueueBoth = useCallback((updater: (q: Card[]) => Card[]) => {
    setQueue((current) => {
      const next = updater(current);
      queueRef.current = next;
      return next;
    });
  }, []);

  const fill = useCallback(async () => {
    if (fillingRef.current) return;
    fillingRef.current = true;
    try {
      while (queueRef.current.length < BUFFER_SIZE) {
        const card = await fetchNextCard(language);
        setQueueBoth((current) => [...current, card]);
      }
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      fillingRef.current = false;
    }
  }, [language, setQueueBoth]);

  useEffect(() => {
    setQueueBoth(() => []);
    fill();
  }, [language, fill, setQueueBoth]);

  const next = useCallback(() => {
    setQueueBoth((current) => current.slice(1));
  }, [setQueueBoth]);

  useEffect(() => {
    if (queue.length < REFILL_THRESHOLD) {
      fill();
    }
  }, [queue.length, fill]);

  return { card: queue[0], next, error };
}
