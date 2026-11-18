import type { SeatStats } from '../api/types'

/** Red = booked, amber = held. The remaining grey is free. */
export function OccupancyBar({ stats }: { stats: SeatStats }) {
  const percent = (value: number) => (stats.total === 0 ? 0 : (value / stats.total) * 100)
  return (
    <div className="bar" role="img" aria-label={`${stats.booked} ocupados, ${stats.held} bloqueados`}>
      <span className="b-booked" style={{ width: `${percent(stats.booked)}%` }} />
      <span className="b-held" style={{ width: `${percent(stats.held)}%` }} />
    </div>
  )
}
