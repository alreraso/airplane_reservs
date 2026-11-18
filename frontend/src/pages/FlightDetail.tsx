import { useEffect, useRef, useState } from 'react'
import { api, ApiError, errorMessage } from '../api/client'
import { isBookable, type FlightEvent, type PassengerData, type Reservation, type SeatEvent } from '../api/types'
import { getClientId } from '../clientId'
import { CheckoutForm } from '../components/CheckoutForm'
import { SeatLegend } from '../components/SeatLegend'
import { SeatMap } from '../components/SeatMap'
import { StatsCards } from '../components/StatsCards'
import { StatusBadge } from '../components/StatusBadge'
import { Ticket } from '../components/Ticket'
import { dateTime, money, STATUS_LABEL } from '../format'
import { useCountdown } from '../hooks/useCountdown'
import { useFlightLive } from '../hooks/useFlightLive'
import { useSeatMap } from '../hooks/useSeatMap'
import { useStreamEvent } from '../realtime/stream'
import { useToast } from '../toast/toast'

const clientId = getClientId()

interface Props {
  flightId: number
  onClose: () => void
}

export function FlightDetail({ flightId, onClose }: Props) {
  const toast = useToast()
  const { flight, stats } = useFlightLive(flightId)
  const { seats, myHold, reload, markHeldByMe, clearMine } = useSeatMap(flightId, clientId)
  const secondsLeft = useCountdown(myHold?.heldUntil ?? null)
  const [ticket, setTicket] = useState<Reservation | null>(null)
  const [busy, setBusy] = useState(false)

  // Seat I am releasing on purpose, so its AVAILABLE event is not reported as an expiry.
  const releasing = useRef<string | null>(null)
  const myHoldRef = useRef(myHold)
  useEffect(() => {
    myHoldRef.current = myHold
  })

  useStreamEvent<SeatEvent>('seat', (event) => {
    const mine = myHoldRef.current
    if (
      event.flightId === flightId &&
      event.status === 'AVAILABLE' &&
      mine?.seatNumber === event.seatNumber &&
      releasing.current !== event.seatNumber
    ) {
      toast(`Tu bloqueo del asiento ${event.seatNumber} expiró y fue liberado`, true)
    }
  })

  useStreamEvent<FlightEvent>('flight', (event) => {
    if (event.flightId === flightId) {
      toast(`El vuelo cambió a: ${STATUS_LABEL[event.status]}`, event.status !== 'SCHEDULED')
    }
  })

  const bookable = flight ? isBookable(flight.status) : false

  async function releaseCurrent(seatNumber: string) {
    releasing.current = seatNumber
    try {
      await api.releaseSeat(flightId, seatNumber, clientId)
      clearMine(seatNumber)
    } finally {
      releasing.current = null
    }
  }

  async function handleSelect(seatNumber: string) {
    setBusy(true)
    try {
      if (myHold) await releaseCurrent(myHold.seatNumber) // one seat per customer at a time
      const hold = await api.holdSeat(flightId, seatNumber, clientId)
      markHeldByMe(seatNumber, hold.heldUntil)
      setTicket(null)
    } catch (e) {
      toast(errorMessage(e), true)
      void reload() // resync: somebody else probably won the race
    } finally {
      setBusy(false)
    }
  }

  async function handleRelease() {
    if (!myHold) return
    setBusy(true)
    try {
      await releaseCurrent(myHold.seatNumber)
    } catch (e) {
      toast(errorMessage(e), true)
    } finally {
      setBusy(false)
    }
  }

  async function handleConfirm(data: PassengerData) {
    if (!myHold) return
    setBusy(true)
    try {
      const reservation = await api.confirmReservation(flightId, {
        ...data,
        clientId,
        seatNumber: myHold.seatNumber,
      })
      clearMine(reservation.seatNumber)
      setTicket(reservation)
      toast('¡Reserva confirmada!')
    } catch (e) {
      toast(errorMessage(e), true)
      // 409: the hold is gone (expired or flight closed). 402 keeps the hold, so no reload.
      if (e instanceof ApiError && e.status === 409) void reload()
    } finally {
      setBusy(false)
    }
  }

  async function handleClose() {
    if (myHold) await releaseCurrent(myHold.seatNumber).catch(() => {})
    onClose()
  }

  if (!flight) {
    return <div className="card">Cargando vuelo…</div>
  }

  return (
    <div className="card" data-testid="flight-detail">
      <div className="row between">
        <h2>
          {flight.flightNumber} · {flight.origin} → {flight.destination}
        </h2>
        <button type="button" className="ghost" onClick={() => void handleClose()}>
          Cerrar
        </button>
      </div>
      <div className="flight-meta">
        <StatusBadge status={flight.status} /> {dateTime(flight.departureTime)} · {money(flight.price)}
      </div>
      {!bookable && (
        <p className="notice" role="alert">
          Este vuelo está {STATUS_LABEL[flight.status].toLowerCase()} y ya no admite reservas.
        </p>
      )}
      <StatsCards stats={stats} />

      <div className="detail-grid">
        <div>
          <SeatLegend />
          <SeatMap seats={seats} canSelect={bookable && !busy} onSelect={(n) => void handleSelect(n)} />
        </div>

        {bookable && myHold && (
          <CheckoutForm
            // A new hold starts with a clean form.
            key={myHold.seatNumber}
            seatNumber={myHold.seatNumber}
            secondsLeft={secondsLeft}
            busy={busy}
            onSubmit={(data) => void handleConfirm(data)}
            onRelease={() => void handleRelease()}
          />
        )}
        {ticket && <Ticket reservation={ticket} />}
      </div>
    </div>
  )
}
