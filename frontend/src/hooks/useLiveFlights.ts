import { useCallback, useEffect, useState } from 'react'
import { api, errorMessage } from '../api/client'
import type { Flight, FlightEvent, SearchParams, SeatStats } from '../api/types'
import { useStreamEvent } from '../realtime/stream'

/**
 * Flights matching a search, plus occupancy stats per flight, kept current by SSE.
 * Used by the list, the dashboard and the admin page.
 */
export function useLiveFlights({ origin, destination, date }: SearchParams) {
  const [flights, setFlights] = useState<Flight[]>([])
  const [stats, setStats] = useState<Record<number, SeatStats>>({})
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    try {
      const list = await api.searchFlights({ origin, destination, date })
      const all = await Promise.all(list.map((flight) => api.getStats(flight.id)))
      setFlights(list)
      setStats(Object.fromEntries(all.map((s) => [s.flightId, s])))
      setError(null)
    } catch (e) {
      setError(errorMessage(e))
    }
  }, [origin, destination, date])

  useEffect(() => {
    // oxlint-disable-next-line react/set-state-in-effect -- fetch on mount; state is set after the await
    void load()
  }, [load])

  // Reload after (re)connecting: events missed while offline are not replayed.
  useStreamEvent('open', () => void load())

  useStreamEvent<SeatStats>('stats', (next) => {
    setStats((previous) => ({ ...previous, [next.flightId]: next }))
  })

  useStreamEvent<FlightEvent>('flight', (event) => {
    setFlights((previous) =>
      previous.map((f) => (f.id === event.flightId ? { ...f, status: event.status } : f)),
    )
  })

  return { flights, stats, error }
}
