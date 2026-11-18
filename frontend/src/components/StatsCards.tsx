import type { SeatStats } from '../api/types'
import { OccupancyBar } from './OccupancyBar'

export function StatsCards({ stats }: { stats: SeatStats | null }) {
  if (!stats) return null
  const cards: Array<[string, number]> = [
    ['Libres', stats.available],
    ['Bloqueados', stats.held],
    ['Ocupados', stats.booked],
  ]
  return (
    <>
      <div className="stats">
        {cards.map(([label, value]) => (
          <div className="stat" key={label}>
            <b>{value}</b>
            {label}
          </div>
        ))}
      </div>
      <OccupancyBar stats={stats} />
    </>
  )
}
