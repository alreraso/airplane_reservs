import type { FlightStatus } from './api/types'

const currency = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  maximumFractionDigits: 0,
})

export const money = (amount: number): string => currency.format(amount)

export const dateTime = (iso: string): string =>
  new Date(iso).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' })

export function mmss(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

export const STATUS_LABEL: Record<FlightStatus, string> = {
  SCHEDULED: 'Programado',
  DELAYED: 'Retrasado',
  CANCELLED: 'Cancelado',
  SOLD_OUT: 'Agotado',
}
