'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import Logo from '@/components/shared/Logo'

const navLinks = [
  { href: '/dashboard', label: 'Dashboard' },
  { href: '/companies', label: 'Companies' },
  { href: '/compare', label: 'Compare' },
  { href: '/portfolio', label: 'Portfolio' },
  { href: '/orders', label: 'Orders' },
  { href: '/watchlist', label: 'Watchlist' },
  { href: '/activity', label: 'Activity' },
]

export default function AppTopbar() {
  const pathname = usePathname()

  return (
    <header className="sticky top-0 z-50 border-b border-[var(--color-border)] bg-[var(--color-page-bg)]/80 backdrop-blur-md">
      <nav className="mx-auto flex h-16 max-w-7xl items-center gap-8 px-6 sm:px-10">
        <Link href="/dashboard" aria-label="QuantEdge home">
          <Logo variant="full" size={22} />
        </Link>

        <div className="flex items-center gap-6">
          {navLinks.map((link) => {
            const active = pathname === link.href || pathname.startsWith(`${link.href}/`)
            return (
              <Link
                key={link.href}
                href={link.href}
                className={`text-sm font-medium transition-colors ${
                  active
                    ? 'text-[var(--color-accent-blue)]'
                    : 'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]'
                }`}
              >
                {link.label}
              </Link>
            )
          })}
        </div>
      </nav>
    </header>
  )
}
