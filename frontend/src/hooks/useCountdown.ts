import { useEffect, useState } from 'react'

/** Seconds left until `until` (ISO instant). Returns 0 when there is nothing to count. */
export function useCountdown(until: string | null): number {
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    if (!until) return
    const id = window.setInterval(() => setNow(Date.now()), 250)
    return () => window.clearInterval(id)
  }, [until])

  if (!until) return 0
  return Math.max(0, Math.floor((new Date(until).getTime() - now) / 1000))
}
