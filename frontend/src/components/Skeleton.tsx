export function SkeletonRows({ label, rows = 6 }: { label: string; rows?: number }) {
  return (
    <div className="table-wrap" role="status">
      <span className="visually-hidden">{label}</span>
      {Array.from({ length: rows }, (_, row) => (
        <div key={row} className="skeleton-row">
          <span className="skeleton skeleton-short" />
          <span className="skeleton" />
          <span className="skeleton skeleton-short" />
        </div>
      ))}
    </div>
  )
}
