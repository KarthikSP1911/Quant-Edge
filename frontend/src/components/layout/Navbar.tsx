'use client'

import { motion } from 'framer-motion'
import Link from 'next/link'
import { useState } from 'react'
import Logo from '@/components/shared/Logo'

export default function Navbar() {
  const [menuOpen, setMenuOpen] = useState(false)

  return (
    <header className="sticky top-0 z-50 border-b border-[var(--color-border)] bg-[var(--color-page-bg)]/80 backdrop-blur-md">
      <nav className="mx-auto flex h-16 max-w-7xl items-center justify-between px-8 sm:px-12 lg:px-20">
        <Link
          href="/"
          aria-label="QuantEdge home"
          className="transition-transform duration-200 hover:scale-105 active:scale-95"
        >
          <Logo variant="full" size={30} />
        </Link>

        <div className="hidden items-center gap-6 md:flex">
          <Link
            href="/login"
            className="text-sm font-medium text-[var(--color-text-secondary)] transition-colors hover:text-[var(--color-text-primary)]"
          >
            Login
          </Link>
          <motion.div whileHover={{ scale: 1.04 }} whileTap={{ scale: 0.96 }}>
            <Link
              href="/register"
              className="rounded-md bg-[var(--color-accent-blue)] px-4 py-2 text-sm font-medium text-white shadow-sm shadow-blue-600/20 transition-colors hover:bg-blue-700"
            >
              Sign Up
            </Link>
          </motion.div>
        </div>

        <button
          type="button"
          onClick={() => setMenuOpen((open) => !open)}
          aria-expanded={menuOpen}
          aria-label="Toggle menu"
          className="flex h-9 w-9 items-center justify-center rounded-md border border-[var(--color-border)] text-[var(--color-text-primary)] transition-colors hover:bg-[var(--color-sidebar-hover)] active:scale-95 md:hidden"
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
        <div className="flex flex-col gap-1 px-8 py-4 sm:px-12">
          <div className="flex flex-col gap-2">
            <Link
              href="/login"
              className="rounded-md px-2 py-2 text-sm font-medium text-[var(--color-text-secondary)] transition-colors hover:bg-[var(--color-sidebar-hover)] hover:text-[var(--color-text-primary)]"
            >
              Login
            </Link>
            <Link
              href="/register"
              className="rounded-md bg-[var(--color-accent-blue)] px-4 py-2 text-center text-sm font-medium text-white transition-transform active:scale-95"
            >
              Sign Up
            </Link>
          </div>
        </div>
      </div>
    </header>
  )
}
