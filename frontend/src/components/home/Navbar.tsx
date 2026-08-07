'use client'

import Link from 'next/link'
import { useState } from 'react'

const navLinks = [
  { href: '#features', label: 'Features' },
  { href: '#docs', label: 'Docs' },
]

export default function Navbar() {
  const [menuOpen, setMenuOpen] = useState(false)

  return (
    <header className="sticky top-0 z-50 border-b border-[var(--color-border)] bg-[var(--color-page-bg)]">
      <nav className="mx-auto flex h-16 max-w-7xl items-center justify-between px-6">
        <Link href="/" className="text-lg font-semibold tracking-tight">
          <span className="text-[var(--color-text-primary)]">Quant</span>
          <span className="text-[var(--color-accent-blue)]">Edge</span>
        </Link>

        <div className="hidden items-center gap-8 md:flex">
          {navLinks.map((link) => (
            <a
              key={link.href}
              href={link.href}
              className="text-sm font-medium text-[var(--color-text-secondary)] transition-colors hover:text-[var(--color-text-primary)]"
            >
              {link.label}
            </a>
          ))}
        </div>

        <div className="hidden items-center gap-6 md:flex">
          <Link
            href="/login"
            className="text-sm font-medium text-[var(--color-text-secondary)] transition-colors hover:text-[var(--color-text-primary)]"
          >
            Login
          </Link>
          <Link
            href="/signup"
            className="rounded-md bg-[var(--color-accent-blue)] px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700"
          >
            Sign Up
          </Link>
        </div>

        <button
          type="button"
          onClick={() => setMenuOpen((open) => !open)}
          aria-expanded={menuOpen}
          aria-label="Toggle menu"
          className="flex h-9 w-9 items-center justify-center rounded-md border border-[var(--color-border)] text-[var(--color-text-primary)] md:hidden"
        >
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none" aria-hidden="true">
            {menuOpen ? (
              <path
                d="M2 2L16 16M16 2L2 16"
                stroke="currentColor"
                strokeWidth="1.5"
                strokeLinecap="round"
              />
            ) : (
              <path
                d="M1 4H17M1 9H17M1 14H17"
                stroke="currentColor"
                strokeWidth="1.5"
                strokeLinecap="round"
              />
            )}
          </svg>
        </button>
      </nav>

      <div
        className={`overflow-hidden border-t border-[var(--color-border)] transition-[max-height] duration-200 ease-in-out md:hidden ${
          menuOpen ? 'max-h-64' : 'max-h-0 border-t-0'
        }`}
      >
        <div className="flex flex-col gap-1 px-6 py-4">
          {navLinks.map((link) => (
            <a
              key={link.href}
              href={link.href}
              onClick={() => setMenuOpen(false)}
              className="rounded-md px-2 py-2 text-sm font-medium text-[var(--color-text-secondary)] hover:bg-[var(--color-sidebar-hover)] hover:text-[var(--color-text-primary)]"
            >
              {link.label}
            </a>
          ))}
          <div className="mt-2 flex flex-col gap-2 border-t border-[var(--color-border)] pt-4">
            <Link
              href="/login"
              className="rounded-md px-2 py-2 text-sm font-medium text-[var(--color-text-secondary)] hover:bg-[var(--color-sidebar-hover)] hover:text-[var(--color-text-primary)]"
            >
              Login
            </Link>
            <Link
              href="/signup"
              className="rounded-md bg-[var(--color-accent-blue)] px-4 py-2 text-center text-sm font-medium text-white"
            >
              Sign Up
            </Link>
          </div>
        </div>
      </div>
    </header>
  )
}
