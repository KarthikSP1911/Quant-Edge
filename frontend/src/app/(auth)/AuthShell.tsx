'use client'

import Link from 'next/link'
import Logo from '@/components/shared/Logo'

export function GoogleIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
      <path
        fill="#4285F4"
        d="M17.64 9.2c0-.64-.06-1.25-.16-1.84H9v3.48h4.84a4.14 4.14 0 0 1-1.8 2.72v2.26h2.9c1.7-1.56 2.7-3.87 2.7-6.62Z"
      />
      <path
        fill="#34A853"
        d="M9 18c2.43 0 4.47-.8 5.96-2.18l-2.9-2.26c-.8.54-1.84.86-3.06.86-2.35 0-4.34-1.59-5.05-3.72H.95v2.33A9 9 0 0 0 9 18Z"
      />
      <path
        fill="#FBBC05"
        d="M3.95 10.7A5.4 5.4 0 0 1 3.67 9c0-.59.1-1.17.28-1.7V4.97H.95A9 9 0 0 0 0 9c0 1.45.35 2.83.95 4.03l3-2.33Z"
      />
      <path
        fill="#EA4335"
        d="M9 3.58c1.32 0 2.51.46 3.44 1.35l2.58-2.58C13.46.89 11.43 0 9 0A9 9 0 0 0 .95 4.97l3 2.33C4.66 5.17 6.65 3.58 9 3.58Z"
      />
    </svg>
  )
}

export function AuthDivider() {
  return (
    <div className="my-3 flex items-center gap-3">
      <div className="h-px flex-1 bg-[var(--color-border)]" />
      <span className="text-xs font-medium text-[var(--color-text-muted)]">OR</span>
      <div className="h-px flex-1 bg-[var(--color-border)]" />
    </div>
  )
}

export function AuthInput({
  label,
  ...props
}: { label: string } & React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <label className="block">
      <span className="mb-1 block text-sm font-medium text-[var(--color-text-primary)]">
        {label}
      </span>
      <input
        {...props}
        className="w-full rounded-md border border-[var(--color-border)] bg-white px-3.5 py-2 text-sm text-[var(--color-text-primary)] shadow-sm outline-none transition-colors placeholder:text-[var(--color-text-muted)] focus:border-[var(--color-accent-blue)] focus:ring-2 focus:ring-[var(--color-accent-light)]"
      />
    </label>
  )
}

export function AuthSubmitButton({
  children,
  isSubmitting,
}: {
  children: React.ReactNode
  isSubmitting: boolean
}) {
  return (
    <button
      type="submit"
      disabled={isSubmitting}
      className="flex w-full items-center justify-center rounded-md bg-[var(--color-accent-blue)] py-2 text-sm font-semibold text-white shadow-sm shadow-blue-600/20 transition-colors hover:bg-blue-700 active:scale-[0.99] disabled:cursor-not-allowed disabled:opacity-60"
    >
      {isSubmitting ? (
        <svg className="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <circle
            className="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            strokeWidth="4"
          />
          <path
            className="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 0 1 8-8V0C5.37 0 0 5.37 0 12h4Z"
          />
        </svg>
      ) : (
        children
      )}
    </button>
  )
}

export function AuthLayout({
  eyebrow,
  title,
  subtitle,
  children,
}: {
  eyebrow: string
  title: string
  subtitle?: string
  children: React.ReactNode
}) {
  return (
    <div className="relative flex h-dvh items-center justify-center overflow-y-auto bg-[var(--color-page-bg)] px-4 py-3">
      <div
        className="pointer-events-none absolute inset-x-0 top-0 -z-10 h-[420px] bg-[radial-gradient(ellipse_60%_50%_at_50%_0%,var(--color-accent-light),transparent)]"
        aria-hidden="true"
      />

      <div className="w-full max-w-md">
        <Link href="/" className="mb-3 flex items-center justify-center">
          <Logo variant="full" size={32} />
        </Link>

        <div className="flex min-h-[490px] flex-col justify-center rounded-2xl border border-[var(--color-border)] bg-white p-7 shadow-xl shadow-slate-900/5">
          <p className="text-xs font-semibold tracking-wide text-[var(--color-accent-blue)] uppercase">
            {eyebrow}
          </p>
          <h1 className="mt-1 text-xl font-semibold text-[var(--color-text-primary)]">{title}</h1>
          {subtitle && (
            <p className="mt-1 text-sm text-[var(--color-text-secondary)]">{subtitle}</p>
          )}

          <div className="mt-3">{children}</div>
        </div>
      </div>
    </div>
  )
}
