'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import { ApiError, googleLoginUrl, register } from '@/lib/auth/api'
import { setAccessToken } from '@/lib/auth/tokens'
import { GoogleIcon, AuthLayout, AuthInput, AuthSubmitButton, AuthDivider } from '../AuthShell'

export default function RegisterPage() {
  const router = useRouter()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setIsSubmitting(true)
    try {
      const data = await register({ name, email, password })
      setAccessToken(data.accessToken)
      router.push('/dashboard')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Registration failed')
      setIsSubmitting(false)
    }
  }

  const handleGoogleLogin = () => {
    window.location.href = googleLoginUrl()
  }

  return (
    <AuthLayout
      eyebrow="Get started"
      title="Create your account"
      subtitle="Free simulated trading with real market data — no card required."
    >
      {error && (
        <div className="mb-4 rounded-md border border-[var(--color-loss)]/30 bg-red-50 px-3 py-2 text-sm text-[var(--color-loss)]">
          {error}
        </div>
      )}

      <form onSubmit={handleRegister} className="space-y-3">
        <AuthInput
          label="Full name"
          type="text"
          autoComplete="name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />
        <AuthInput
          label="Email"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <AuthInput
          label="Password"
          type="password"
          autoComplete="new-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        <AuthSubmitButton isSubmitting={isSubmitting}>Create Account</AuthSubmitButton>
      </form>

      <AuthDivider />

      <button
        type="button"
        onClick={handleGoogleLogin}
        className="flex w-full items-center justify-center gap-2.5 rounded-md border border-[var(--color-border)] bg-white py-2 text-sm font-medium text-[var(--color-text-primary)] transition-colors hover:bg-[var(--color-sidebar-hover)] active:scale-[0.99]"
      >
        <GoogleIcon />
        Continue with Google
      </button>

      <p className="mt-5 text-center text-sm text-[var(--color-text-secondary)]">
        Already have an account?{' '}
        <Link href="/login" className="font-medium text-[var(--color-accent-blue)] hover:underline">
          Sign in
        </Link>
      </p>
    </AuthLayout>
  )
}
