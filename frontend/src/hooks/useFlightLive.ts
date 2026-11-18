import { useCallback, useEffect, useState } from 'react'
import { api, errorMessage } from '../api/client'
import type { Flight, FlightEvent, SeatStats } from '../api/types'
import { useStreamEvent } from '../realtime/stream'
import { useToast } from '../toast/toast'

/** One flight and its stats, kept current by SSE. */
export function useFlightLive(flightId: number) {
  const toast = useToast()
  const [flight, setFlight] = useState<Flight | null>(null)
  const [stats, setStats] = useState<SeatStats | null>(null)

  const load = useCallback(async () => {
    try {
      const [loadedFlight, loadedStats] = await Promise.all([
        api.getFlight(flightId),
        api.getStats(flightId),
      ])
      setFlight(loadedFlight)
      setStats(loadedStats)
    } catch (e) {
      toast(errorMessage(e), true)
    }
  }, [flightId, toast])

  useEffect(() => {
    // oxlint-disable-next-line react/set-state-in-effect -- fetch on mount; state is set after the await
    void load()
  }, [load])

  useStreamEvent('open', () => void load())

  useStreamEvent<SeatStats>('stats', (next) => {
    if (next.flightId === flightId) setStats(next)
  })

  useStreamEvent<FlightEvent>('flight', (event) => {
    if (event.flightId !== flightId) return
    setFlight((previous) => (previous ? { ...previous, status: event.status } : previous))
  })

  return { flight, stats }
}
