const ITEMS: Array<[string, string]> = [
  ['available', 'Libre'],
  ['held', 'Bloqueado'],
  ['mine', 'Tuyo'],
  ['booked', 'Ocupado'],
]

export function SeatLegend() {
  return (
    <div className="legend">
      {ITEMS.map(([css, label]) => (
        <span key={css}>
          <i className={`seat ${css}`} />
          {label}
        </span>
      ))}
    </div>
  )
}
