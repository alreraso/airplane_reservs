import type { Seat } from '../api/types'

interface Props {
  seats: Seat[]
  /** False when the flight is not bookable or an action is in progress. */
  canSelect: boolean
  onSelect: (seatNumber: string) => void
}

const LEFT_BLOCK = 'ABC'

function groupByRow(seats: Seat[]): Array<[number, Seat[]]> {
  const rows = new Map<number, Seat[]>()
  for (const seat of seats) {
    const row = parseInt(seat.seatNumber, 10)
    rows.set(row, [...(rows.get(row) ?? []), seat])
  }
  return [...rows.entries()]
}

function seatClass(seat: Seat): string {
  return seat.heldByMe && seat.status === 'HELD' ? 'mine' : seat.status.toLowerCase()
}

export function SeatMap({ seats, canSelect, onSelect }: Props) {
  const renderSeat = (seat: Seat) => (
    <button
      key={seat.seatNumber}
      type="button"
      className={`seat ${seatClass(seat)}`}
      disabled={!(canSelect && seat.status === 'AVAILABLE')}
      title={`${seat.seatNumber} · ${seat.status}`}
      onClick={() => onSelect(seat.seatNumber)}
    >
      {seat.seatNumber}
    </button>
  )

  return (
    <div className="seat-map" role="group" aria-label="Mapa de asientos">
      {groupByRow(seats).map(([row, rowSeats]) => (
        <div className="seat-row" key={row}>
          <span className="row-label">{row}</span>
          {rowSeats.filter((s) => LEFT_BLOCK.includes(s.seatNumber.slice(-1))).map(renderSeat)}
          <span className="aisle" />
          {rowSeats.filter((s) => !LEFT_BLOCK.includes(s.seatNumber.slice(-1))).map(renderSeat)}
        </div>
      ))}
    </div>
  )
}
