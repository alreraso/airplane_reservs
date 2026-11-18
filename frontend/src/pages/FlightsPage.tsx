import { useState, type FormEvent } from 'react'
import type { SearchParams } from '../api/types'
import { StatusBadge } from '../components/StatusBadge'
import { dateTime, money } from '../format'
import { useLiveFlights } from '../hooks/useLiveFlights'
import { FlightDetail } from './FlightDetail'

const EMPTY_SEARCH: SearchParams = { origin: '', destination: '', date: '' }

export function FlightsPage() {
  const [params, setParams] = useState<SearchParams>(EMPTY_SEARCH)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const { flights, stats, error } = useLiveFlights(params)

  const search = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const form = new FormData(e.currentTarget)
    setParams({
      origin: String(form.get('origin') ?? ''),
      destination: String(form.get('destination') ?? ''),
      date: String(form.get('date') ?? ''),
    })
  }

  return (
    <section>
      <form className="card row" onSubmit={search}>
        <label>
          Origen <input name="origin" maxLength={3} placeholder="BOG" />
        </label>
        <label>
          Destino <input name="destination" maxLength={3} placeholder="MDE" />
        </label>
        <label>
          Fecha <input name="date" type="date" />
        </label>
        <button type="submit">Buscar</button>
      </form>

      {error && <div className="empty">{error}</div>}
      {!error && flights.length === 0 && <div className="empty">No hay vuelos para esa búsqueda.</div>}

      <div data-testid="flight-list">
        {flights.map((flight) => (
          <button
            type="button"
            key={flight.id}
            className="card flight"
            data-testid={`flight-${flight.flightNumber}`}
            onClick={() => setSelectedId(flight.id)}
          >
            <b>{flight.flightNumber}</b>
            <span>
              {flight.origin} → {flight.destination} · {dateTime(flight.departureTime)}
            </span>
            <span>{money(flight.price)}</span>
            <span>{stats[flight.id]?.available ?? flight.availableSeats} asientos libres</span>
            <StatusBadge status={flight.status} />
          </button>
        ))}
      </div>

      {selectedId !== null && (
        // key: opening another flight resets all of its local state.
        <FlightDetail key={selectedId} flightId={selectedId} onClose={() => setSelectedId(null)} />
      )}
    </section>
  )
}
