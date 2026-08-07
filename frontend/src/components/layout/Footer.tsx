import Logo from '@/components/shared/Logo'

const columns = [
  {
    heading: 'Product',
    links: [
      { label: 'Features', href: '#features' },
      { label: 'Docs', href: '#docs' },
    ],
  },
  {
    heading: 'Company',
    links: [
      { label: 'About', href: '#' },
      { label: 'Contact', href: '#' },
    ],
  },
  {
    heading: 'Legal',
    links: [
      { label: 'Privacy', href: '#' },
      { label: 'Terms', href: '#' },
    ],
  },
]

export default function Footer() {
  return (
    <footer className="border-t border-[var(--color-border)]">
      <div className="mx-auto max-w-7xl px-6 py-12">
        <div className="flex flex-col gap-10 sm:flex-row sm:justify-between">
          <div>
            <Logo variant="full" size={22} />
            <p className="mt-2 max-w-xs text-sm text-[var(--color-text-secondary)]">
              Simulated trading with real market data.
            </p>
          </div>

          <div className="grid grid-cols-3 gap-8 sm:gap-16">
            {columns.map((column) => (
              <div key={column.heading}>
                <p className="text-sm font-semibold text-[var(--color-text-primary)]">
                  {column.heading}
                </p>
                <ul className="mt-3 space-y-2">
                  {column.links.map((link) => (
                    <li key={link.label}>
                      <a
                        href={link.href}
                        className="text-sm text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]"
                      >
                        {link.label}
                      </a>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </div>

        <p className="mt-12 border-t border-[var(--color-border)] pt-6 text-sm text-[var(--color-text-muted)]">
          © {new Date().getFullYear()} QuantEdge. All rights reserved.
        </p>
      </div>
    </footer>
  )
}
