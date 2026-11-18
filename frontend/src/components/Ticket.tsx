import type { Reservation } from '../api/types'
import { money } from '../format'
import { CopyButton } from './CopyButton'

export function Ticket({ reservation }: { reservation: Reservation }) {
  return (
    <div className="ticket" data-testid="ticket">
      <div>Reserva confirmada</div>
      <div className="code-row">
        <span className="code" data-testid="reservation-code">
          {reservation.code}
        </span>
        <CopyButton text={reservation.code} />
      </div>
      <div>
        {reservation.passengerName} · asiento {reservation.seatNumber} · {money(reservation.amount)}
      </div>
      <p className="hint">Guarda este código: en la pestaña «Mi reserva» puedes consultarla cuando quieras.</p>
    </div>
  )
}
