'use client'

import { Suspense, useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import Link from 'next/link'
import { ApiError, googleLoginUrl, login } from '@/lib/auth/api'
import { setAccessToken } from '@/lib/auth/tokens'
import Logo from '@/components/shared/Logo'
import { GoogleIcon, AuthLayout, AuthInput, AuthSubmitButton, AuthDivider } from '../AuthShell'

const OAUTH_ERROR_MESSAGES: Record<string, string> = {
  EmailNotProvided:
    'Your Google account did not share an email address. Please try another sign-in method.',
}

function LoginForm() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const oauthError = searchParams.get('error')
  const [error, setError] = useState(
    oauthError
      ? (OAUTH_ERROR_MESSAGES[oauthError] ?? 'Google sign-in failed. Please try again.')
      : '',
  )

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setIsSubmitting(true)
    try {
      const data = await login({ email, password })
      setAccessToken(data.accessToken)
      router.push('/dashboard')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Invalid email or password')
      setIsSubmitting(false)
    }
  }

  const handleGoogleLogin = () => {
    window.location.href = googleLoginUrl()
  }

  return (
    <AuthLayout
      eyebrow="Welcome back"
      title="Sign in to QuantEdge"
      subtitle="Pick up your research and simulated portfolio right where you left off."
    >
      {error && (
        <div className="mb-4 rounded-md border border-[var(--color-loss)]/30 bg-red-50 px-3 py-2 text-sm text-[var(--color-loss)]">
          {error}
        </div>
      )}

      <form onSubmit={handleLogin} className="space-y-3">
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
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        <AuthSubmitButton isSubmitting={isSubmitting}>Sign In</AuthSubmitButton>
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
        Don&apos;t have an account?{' '}
        <Link
          href="/register"
          className="font-medium text-[var(--color-accent-blue)] hover:underline"
        >
          Sign up
        </Link>
      </p>
    </AuthLayout>
  )
}

export default function LoginPage() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-screen items-center justify-center bg-[var(--color-card-bg)]">
          <Logo variant="mark" size={28} />
        </div>
      }
    >
      <LoginForm />
    </Suspense>
  )
}
