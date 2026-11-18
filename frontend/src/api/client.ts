import type {
  Flight,
  FlightStatus,
  HoldResponse,
  Reservation,
  ReservationDetail,
  ReservationRequest,
  SearchParams,
  Seat,
  SeatStats,
} from './types'

export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: { 'Content-Type': 'application/json' },
  })
  if (response.status === 204) {
    return undefined as T
  }
  const body: unknown = await response.json().catch(() => ({}))
  if (!response.ok) {
    // Errors come as RFC 9457 ProblemDetail; "detail" holds the human message.
    const detail = (body as { detail?: string }).detail
    throw new ApiError(detail ?? `Error ${response.status}`, response.status)
  }
  return body as T
}

const flightPath = (flightId: number) => `/api/flights/${flightId}`

export const api = {
  searchFlights(params: SearchParams): Promise<Flight[]> {
    const query = new URLSearchParams()
    for (const [key, value] of Object.entries(params)) {
      if (value) query.set(key, value)
    }
    return request(`/api/flights?${query.toString()}`)
  },

  getFlight(flightId: number): Promise<Flight> {
    return request(flightPath(flightId))
  },

  getStats(flightId: number): Promise<SeatStats> {
    return request(`${flightPath(flightId)}/stats`)
  },

  getSeats(flightId: number, clientId: string): Promise<Seat[]> {
    return request(`${flightPath(flightId)}/seats?clientId=${encodeURIComponent(clientId)}`)
  },

  holdSeat(flightId: number, seatNumber: string, clientId: string): Promise<HoldResponse> {
    return request(`${flightPath(flightId)}/seats/${seatNumber}/hold`, {
      method: 'POST',
      body: JSON.stringify({ clientId }),
    })
  },

  releaseSeat(flightId: number, seatNumber: string, clientId: string): Promise<void> {
    return request(
      `${flightPath(flightId)}/seats/${seatNumber}/hold?clientId=${encodeURIComponent(clientId)}`,
      { method: 'DELETE' },
    )
  },

  confirmReservation(flightId: number, data: ReservationRequest): Promise<Reservation> {
    return request(`${flightPath(flightId)}/reservations`, {
      method: 'POST',
      body: JSON.stringify(data),
    })
  },

  getReservation(code: string): Promise<ReservationDetail> {
    return request(`/api/reservations/${encodeURIComponent(code.trim())}`)
  },

  changeFlightStatus(flightId: number, status: FlightStatus): Promise<Flight> {
    return request(`${flightPath(flightId)}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    })
  },
}

export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'Error inesperado'
}
