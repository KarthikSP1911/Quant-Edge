'use client'

import { Suspense, useEffect, useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { ApiError, exchangeOAuthCode } from '@/lib/auth/api'
import { setAccessToken } from '@/lib/auth/tokens'
import { clearChatHistory } from '@/lib/api/chat'

function OAuth2Callback() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const code = searchParams.get('code')
    if (!code) {
      return
    }

    const exchange = async () => {
      try {
        const data = await exchangeOAuthCode(code)
        setAccessToken(data.accessToken)
        // Best-effort - a fresh login starts a clean chat conversation, but shouldn't block on it.
        void clearChatHistory().catch(() => {})
        router.push('/dashboard')
      } catch (err) {
        setError(err instanceof ApiError ? err.message : 'Authentication failed. Please try again.')
      }
    }

    void exchange()
  }, [router, searchParams])

  const code = searchParams.get('code')
  if (!code) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[var(--color-page-bg)]">
        <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-8 text-center text-[var(--color-loss)] shadow-sm">
          No authorization code provided
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[var(--color-page-bg)]">
        <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-8 text-center text-[var(--color-loss)] shadow-sm">
          {error}
        </div>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-[var(--color-page-bg)]">
      <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-8 text-center text-[var(--color-text-primary)] shadow-sm">
        Authenticating...
      </div>
    </div>
  )
}

export default function OAuth2CallbackPage() {
  return (
    <Suspense fallback={null}>
      <OAuth2Callback />
    </Suspense>
  )
}
