'use client'

import { useEffect, useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { ApiError, exchangeOAuthCode } from '@/lib/auth/api'
import { setAccessToken } from '@/lib/auth/tokens'

export default function OAuth2CallbackPage() {
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
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="p-8 bg-white rounded shadow text-center text-red-600">
          No authorization code provided
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="p-8 bg-white rounded shadow text-center text-red-600">{error}</div>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="p-8 bg-white rounded shadow text-center">Authenticating...</div>
    </div>
  )
}
