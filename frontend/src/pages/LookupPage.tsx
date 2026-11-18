import { useState, type FormEvent } from 'react'
import { api, ApiError, errorMessage } from '../api/client'
import type { FlightEvent, ReservationDetail } from '../api/types'
import { CopyButton } from '../components/CopyButton'
import { StatusBadge } from '../components/StatusBadge'
import { dateTime, money, STATUS_LABEL } from '../format'
import { useStreamEvent } from '../realtime/stream'

/** Recover a ticket from its code, for example after closing the confirmation screen. */
export function LookupPage() {
  const [code, setCode] = useState('')
  const [reservation, setReservation] = useState<ReservationDetail | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  // The flight of a ticket on screen can change (cancelled, delayed): keep it current.
  useStreamEvent<FlightEvent>('flight', (event) => {
    setReservation((current) =>
      current && current.flight.id === event.flightId
        ? { ...current, flight: { ...current.flight, status: event.status } }
        : current,
    )
  })

  async function search(e: FormEvent) {
    e.preventDefault()
    setLoading(true)
    setError(null)
    setReservation(null)
    try {
      setReservation(await api.getReservation(code))
    } catch (err) {
      setError(
        err instanceof ApiError && err.status === 404
          ? 'No encontramos una reserva con ese código. Revisa que esté completo (RSV-XXXXXXXX).'
          : errorMessage(err),
      )
    } finally {
      setLoading(false)
    }
  }

  const flight = reservation?.flight
  const flightClosed = flight && (flight.status === 'CANCELLED' || flight.status === 'SOLD_OUT')

  return (
    <section>
      <form className="card row" onSubmit={(e) => void search(e)}>
        <label>
          Código de reserva
          <input
            name="code"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            placeholder="RSV-1A2B3C4D"
            autoComplete="off"
            required
          />
        </label>
        <button type="submit" disabled={loading}>
          Consultar
        </button>
      </form>

      {error && (
        <div className="notice" role="alert" data-testid="lookup-error">
          {error}
        </div>
      )}

      {reservation && flight && (
        <div className="card ticket" data-testid="lookup-result">
          <div className="code-row">
            <span className="code">{reservation.code}</span>
            <CopyButton text={reservation.code} />
          </div>
          {flightClosed && (
            <p className="notice" role="alert">
              Tu vuelo está {STATUS_LABEL[flight.status].toLowerCase()}.
            </p>
          )}
          <dl className="detail-list">
            <dt>Vuelo</dt>
            <dd>
              {flight.flightNumber} · {flight.origin} → {flight.destination} <StatusBadge status={flight.status} />
            </dd>
            <dt>Salida</dt>
            <dd>{dateTime(flight.departureTime)}</dd>
            <dt>Asiento</dt>
            <dd>{reservation.seatNumber}</dd>
            <dt>Pasajero</dt>
            <dd>{reservation.passengerName}</dd>
            <dt>Email</dt>
            <dd>{reservation.maskedEmail}</dd>
            <dt>Total pagado</dt>
            <dd>{money(reservation.amount)}</dd>
            <dt>Reservado el</dt>
            <dd>{dateTime(reservation.createdAt)}</dd>
          </dl>
        </div>
      )}
    </section>
  )
}
