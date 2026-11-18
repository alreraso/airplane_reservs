import { OccupancyBar } from '../components/OccupancyBar'
import { StatusBadge } from '../components/StatusBadge'
import { useLiveFlights } from '../hooks/useLiveFlights'

const ALL_FLIGHTS = { origin: '', destination: '', date: '' }

/** Story 4: occupancy of every flight, updated on each hold, expiry and booking. */
export function DashboardPage() {
  const { flights, stats } = useLiveFlights(ALL_FLIGHTS)

  return (
    <section className="card">
      <h2>Ocupación en tiempo real</h2>
      {flights.map((flight) => {
        const s = stats[flight.id]
        if (!s) return null
        return (
          <div className="dash-row" key={flight.id} data-testid={`dash-${flight.flightNumber}`}>
            <div>
              <b>{flight.flightNumber}</b> {flight.origin} → {flight.destination}{' '}
              <StatusBadge status={flight.status} />
            </div>
            <OccupancyBar stats={s} />
            <div>
              Libres {s.available} · Bloqueados {s.held} · Ocupados {s.booked}
            </div>
          </div>
        )
      })}
    </section>
  )
}
