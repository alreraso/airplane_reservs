import { useCallback, useEffect, useState } from 'react'
import { api, errorMessage } from '../api/client'
import type { Seat, SeatEvent } from '../api/types'
import { useStreamEvent } from '../realtime/stream'
import { useToast } from '../toast/toast'

/**
 * Seat map of one flight. The server is the source of truth: REST loads it, and every
 * "seat" event patches a single seat. The `heldByMe` flag is the only thing events cannot
 * tell us (the server never reveals who holds a seat), so local actions set it explicitly.
 */
export function useSeatMap(flightId: number, clientId: string) {
  const toast = useToast()
  const [seats, setSeats] = useState<Seat[]>([])

  const reload = useCallback(async () => {
    try {
      setSeats(await api.getSeats(flightId, clientId))
    } catch (e) {
      toast(errorMessage(e), true)
    }
  }, [flightId, clientId, toast])

  useEffect(() => {
    // oxlint-disable-next-line react/set-state-in-effect -- fetch on mount; state is set after the await
    void reload()
  }, [reload])

  useStreamEvent('open', () => void reload())

  useStreamEvent<SeatEvent>('seat', (event) => {
    if (event.flightId !== flightId) return
    setSeats((previous) =>
      previous.map((seat) =>
        seat.seatNumber === event.seatNumber
          ? {
              ...seat,
              status: event.status,
              heldUntil: event.heldUntil,
              // Ownership only survives while the seat stays HELD.
              heldByMe: event.status === 'HELD' && seat.heldByMe,
            }
          : seat,
      ),
    )
  })

  const markHeldByMe = useCallback((seatNumber: string, heldUntil: string) => {
    setSeats((previous) =>
      previous.map((seat) =>
        seat.seatNumber === seatNumber
          ? { ...seat, status: 'HELD', heldUntil, heldByMe: true }
          : seat,
      ),
    )
  }, [])

  const clearMine = useCallback((seatNumber: string) => {
    setSeats((previous) =>
      previous.map((seat) =>
        seat.seatNumber === seatNumber ? { ...seat, heldByMe: false } : seat,
      ),
    )
  }, [])

  const myHold = seats.find((seat) => seat.heldByMe && seat.status === 'HELD') ?? null

  return { seats, myHold, reload, markHeldByMe, clearMine }
}
