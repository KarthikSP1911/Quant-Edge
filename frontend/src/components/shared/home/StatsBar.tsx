const stats = [
  { value: '$10,000', label: 'Starting balance' },
  { value: 'Real-time', label: 'Market data' },
  { value: '4', label: 'Order types' },
  { value: '11', label: 'Tracked data tables' },
]

export default function StatsBar() {
  return (
    <section className="border-y border-[var(--color-border)] bg-[var(--color-card-bg)]">
      <div className="mx-auto grid max-w-7xl grid-cols-2 divide-x divide-[var(--color-border)] px-6 sm:grid-cols-4">
        {stats.map((stat) => (
          <div key={stat.label} className="px-4 py-8 text-center">
            <p className="text-2xl font-semibold text-[var(--color-text-primary)]">{stat.value}</p>
            <p className="mt-1 text-sm text-[var(--color-text-secondary)]">{stat.label}</p>
          </div>
        ))}
      </div>
    </section>
  )
}
