import Link from 'next/link'

const actions = [
  { href: '/companies', label: 'Browse companies', description: 'Discover and research stocks' },
  { href: '/research', label: 'Ask research', description: 'Get an AI research report' },
  { href: '/orders', label: 'Place an order', description: 'Buy or sell from a stock page' },
]

export default function QuickActions() {
  return (
    <section>
      <h2 className="mb-4 text-lg font-semibold text-[var(--color-text-primary)]">Quick actions</h2>
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        {actions.map((action) => (
          <Link
            key={action.href}
            href={action.href}
            className="rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-4 transition-colors hover:bg-[var(--color-sidebar-hover)]"
          >
            <div className="font-medium text-[var(--color-text-primary)]">{action.label}</div>
            <div className="mt-1 text-xs text-[var(--color-text-secondary)]">
              {action.description}
            </div>
          </Link>
        ))}
      </div>
    </section>
  )
}
