// Types mirror docs/shared/openapi.yaml. If the API changes, change them here first.

export type FlightStatus = 'SCHEDULED' | 'DELAYED' | 'CANCELLED' | 'SOLD_OUT'
export type SeatStatus = 'AVAILABLE' | 'HELD' | 'BOOKED'

export interface Flight {
  id: number
  flightNumber: string
  origin: string
  destination: string
  departureTime: string
  arrivalTime: string
  price: number
  status: FlightStatus
  availableSeats: number
}

export interface Seat {
  seatNumber: string
  status: SeatStatus
  heldUntil: string | null
  heldByMe: boolean
}

export interface SeatStats {
  flightId: number
  total: number
  available: number
  held: number
  booked: number
}

export interface HoldResponse {
  seatNumber: string
  heldUntil: string
}

export interface PassengerData {
  passengerName: string
  email: string
  cardNumber: string
}

export interface ReservationRequest extends PassengerData {
  clientId: string
  seatNumber: string
}

export interface Reservation {
  code: string
  flightId: number
  seatNumber: string
  passengerName: string
  amount: number
  createdAt: string
}

/** Ticket recovered by its code. The email arrives masked (a***@example.com). */
export interface ReservationDetail {
  code: string
  passengerName: string
  maskedEmail: string
  seatNumber: string
  amount: number
  createdAt: string
  flight: {
    id: number
    flightNumber: string
    origin: string
    destination: string
    departureTime: string
    arrivalTime: string
    status: FlightStatus
  }
}

export interface SearchParams {
  origin: string
  destination: string
  date: string
}

// Payloads of the Server-Sent Events
export interface SeatEvent {
  flightId: number
  seatNumber: string
  status: SeatStatus
  heldUntil: string | null
}

export interface FlightEvent {
  flightId: number
  status: FlightStatus
}

export function isBookable(status: FlightStatus): boolean {
  return status === 'SCHEDULED' || status === 'DELAYED'
}
