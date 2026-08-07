const features = [
  {
    title: 'AI research agent',
    description:
      'A 5-step agent with 9 tools analyzes a company and streams its reasoning trace in real time.',
  },
  {
    title: 'Order matching engine',
    description:
      'Market, limit, stop-loss, and stop-limit orders are matched against live prices through a Kafka-backed engine.',
  },
  {
    title: 'Portfolio time machine',
    description:
      'Replay your portfolio at any past date to see exactly what it was worth and why it moved.',
  },
  {
    title: 'Stock comparison',
    description:
      'Compare fundamentals, price performance, and indicators across multiple companies side by side.',
  },
  {
    title: 'Audit log',
    description:
      'Every trade, order change, and account action is recorded automatically via AOP-based auditing.',
  },
  {
    title: 'PDF and CSV exports',
    description:
      'Export portfolio statements, transaction history, and research notes for offline review.',
  },
]

export default function FeatureGrid() {
  return (
    <section id="features" className="mx-auto max-w-7xl px-6 py-20">
      <div className="max-w-2xl">
        <h2 className="text-3xl font-semibold tracking-tight text-[var(--color-text-primary)]">
          Built for serious research
        </h2>
        <p className="mt-3 text-lg text-[var(--color-text-secondary)]">
          Every feature is designed around a single question: does this help you make a better
          decision.
        </p>
      </div>

      <div className="mt-12 grid grid-cols-1 gap-px overflow-hidden rounded-lg border border-[var(--color-border)] bg-[var(--color-border)] sm:grid-cols-2 lg:grid-cols-3">
        {features.map((feature) => (
          <div key={feature.title} className="bg-[var(--color-card-bg)] p-6">
            <h3 className="text-base font-semibold text-[var(--color-text-primary)]">
              {feature.title}
            </h3>
            <p className="mt-2 text-sm text-[var(--color-text-secondary)]">{feature.description}</p>
          </div>
        ))}
      </div>
    </section>
  )
}
