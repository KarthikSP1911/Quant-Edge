'use client'

import { useEffect, useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
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

    const exchangeCode = async () => {
      try {
        const response = await fetch('http://localhost:8080/api/auth/oauth2/callback', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          credentials: 'include',
          body: JSON.stringify({ code }),
        })

        if (!response.ok) {
          throw new Error('Failed to exchange code')
        }

        const data = await response.json()
        setAccessToken(data.accessToken)
        router.push('/dashboard')
      } catch {
        setError('Authentication failed. Please try again.')
      }
    }

    void exchangeCode()
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
