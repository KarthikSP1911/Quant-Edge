'use client'

import { useRef, useState } from 'react'
import { useRouter } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { getCurrentUser, logout } from '@/lib/auth/api'
import { clearAccessToken, getAccessToken } from '@/lib/auth/tokens'
import { useOutsideClick } from '@/hooks/useOutsideClick'

function initials(name: string) {
  const parts = name.trim().split(/\s+/)
  const first = parts[0]?.[0] ?? ''
  const last = parts.length > 1 ? (parts[parts.length - 1]?.[0] ?? '') : ''
  return (first + last).toUpperCase() || '?'
}

export default function UserMenu() {
  const router = useRouter()
  const [open, setOpen] = useState(false)
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  const { data: user } = useQuery({
    queryKey: ['currentUser'],
    queryFn: () => {
      const token = getAccessToken()
      if (!token) throw new Error('No access token')
      return getCurrentUser(token)
    },
    retry: false,
    staleTime: 5 * 60 * 1000,
  })

  useOutsideClick(menuRef, open, () => setOpen(false))

  const handleLogout = async () => {
    setIsLoggingOut(true)
    try {
      await logout()
    } catch {
      // proceed with local sign-out regardless of API result
    } finally {
      clearAccessToken()
      router.push('/login')
    }
  }

  if (!user) {
    return <div className="h-9 w-9 animate-pulse rounded-full bg-[var(--color-sidebar-hover)]" />
  }

  return (
    <div className="relative" ref={menuRef}>
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-expanded={open}
        aria-label="Account menu"
        className="flex h-9 w-9 items-center justify-center rounded-full bg-[var(--color-accent-light)] text-xs font-semibold text-[var(--color-accent-blue)] transition-transform hover:scale-105 active:scale-95"
      >
        {initials(user.name)}
      </button>

      {open && (
        <div className="absolute right-0 z-50 mt-2 w-56 overflow-hidden rounded-lg border border-[var(--color-border)] bg-white shadow-lg shadow-slate-900/10">
          <div className="border-b border-[var(--color-border)] px-4 py-3">
            <p className="truncate text-sm font-medium text-[var(--color-text-primary)]">
              {user.name}
            </p>
            <p className="truncate text-xs text-[var(--color-text-secondary)]">{user.email}</p>
          </div>
          <button
            type="button"
            onClick={() => void handleLogout()}
            disabled={isLoggingOut}
            className="flex w-full items-center gap-2 px-4 py-2.5 text-left text-sm font-medium text-[var(--color-loss)] transition-colors hover:bg-[var(--color-sidebar-hover)] disabled:opacity-60"
          >
            {isLoggingOut ? 'Signing out…' : 'Sign out'}
          </button>
        </div>
      )}
    </div>
  )
}
