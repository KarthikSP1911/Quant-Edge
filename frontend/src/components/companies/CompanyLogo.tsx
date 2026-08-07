'use client'

import { useState } from 'react'

export default function CompanyLogo({
  symbol,
  logoUrl,
}: {
  symbol: string
  logoUrl: string | null
}) {
  const [failed, setFailed] = useState(false)

  if (!logoUrl || failed) {
    return (
      <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[var(--color-accent-light)] text-xs font-semibold text-[var(--color-accent-blue)]">
        {symbol.slice(0, 2)}
      </div>
    )
  }

  return (
    // eslint-disable-next-line @next/next/no-img-element -- external, unconfigured logo domains
    <img
      src={logoUrl}
      alt=""
      width={36}
      height={36}
      className="h-9 w-9 shrink-0 rounded-full border border-[var(--color-border)] bg-white object-contain p-1"
      onError={() => setFailed(true)}
    />
  )
}
