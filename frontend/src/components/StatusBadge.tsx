import type { FlightStatus } from '../api/types'
import { STATUS_LABEL } from '../format'

export function StatusBadge({ status }: { status: FlightStatus }) {
  return <span className={`badge ${status}`}>{STATUS_LABEL[status]}</span>
}
