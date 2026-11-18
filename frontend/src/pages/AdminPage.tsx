import { useState } from 'react'
import { api, errorMessage } from '../api/client'
import type { Flight, FlightEvent, FlightStatus, SeatEvent } from '../api/types'
import { StatusBadge } from '../components/StatusBadge'
import { dateTime, STATUS_LABEL } from '../format'
import { useLiveFlights } from '../hooks/useLiveFlights'
import { useStreamEvent } from '../realtime/stream'
import { useToast } from '../toast/toast'

const ALL_FLIGHTS = { origin: '', destination: '', date: '' }
const ACTIONS: FlightStatus[] = ['SCHEDULED', 'DELAYED', 'CANCELLED']
const MAX_LOG = 20

interface LogEntry {
  id: number
  text: string
}

let nextLogId = 1

/**
 * Simulation panel: change a flight status and watch every open client react.
 * It has no authentication on purpose (see docs/shared/decisions.md, section 10).
 */
export function AdminPage() {
  const toast = useToast()
  const { flights } = useLiveFlights(ALL_FLIGHTS)
  const [log, setLog] = useState<LogEntry[]>([])

  const label = (flightId: number, list: Flight[]) =>
    list.find((f) => f.id === flightId)?.flightNumber ?? `vuelo ${flightId}`

  const addLog = (text: string) =>
    setLog((previous) => [{ id: nextLogId++, text }, ...previous].slice(0, MAX_LOG))

  useStreamEvent<SeatEvent>('seat', (e) =>
    addLog(`${label(e.flightId, flights)} · asiento ${e.seatNumber} → ${e.status}`),
  )
  useStreamEvent<FlightEvent>('flight', (e) =>
    addLog(`${label(e.flightId, flights)} · vuelo → ${STATUS_LABEL[e.status]}`),
  )

  async function change(flight: Flight, status: FlightStatus) {
    try {
      await api.changeFlightStatus(flight.id, status)
    } catch (e) {
      toast(errorMessage(e), true)
    }
  }

  return (
    <section>
      <div className="card">
        <h2>Administración de vuelos (simulación)</h2>
        <p className="hint">Sin autenticación a propósito. Los cambios llegan en vivo a todas las pestañas abiertas.</p>
        {flights.map((flight) => (
          <div className="dash-row admin-row" key={flight.id} data-testid={`admin-${flight.flightNumber}`}>
            <div>
              <b>{flight.flightNumber}</b> {flight.origin} → {flight.destination}
              <div className="hint">{dateTime(flight.departureTime)}</div>
            </div>
            <StatusBadge status={flight.status} />
            <div className="row">
              {ACTIONS.map((status) => (
                <button
                  type="button"
                  key={status}
                  className="ghost"
                  disabled={flight.status === status}
                  onClick={() => void change(flight, status)}
                >
                  {STATUS_LABEL[status]}
                </button>
              ))}
            </div>
          </div>
        ))}
      </div>

      <div className="card">
        <h3>Eventos recientes</h3>
        {log.length === 0 && <p className="hint">Aún no ha llegado ningún evento.</p>}
        <ul className="log" data-testid="event-log">
          {log.map((entry) => (
            <li key={entry.id}>{entry.text}</li>
          ))}
        </ul>
      </div>
    </section>
  )
}
