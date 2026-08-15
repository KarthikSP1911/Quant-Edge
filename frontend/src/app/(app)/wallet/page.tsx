'use client'

import { Suspense, useEffect } from 'react'
import { useSearchParams } from 'next/navigation'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import TopUpForm from '@/components/wallet/TopUpForm'
import WalletTransactionsTable from '@/components/wallet/WalletTransactionsTable'

function WalletStatusListener() {
  const searchParams = useSearchParams()
  const queryClient = useQueryClient()

  useEffect(() => {
    const status = searchParams.get('status')
    if (status === 'success') {
      toast.success('Wallet topped up successfully')
      void queryClient.invalidateQueries({ queryKey: ['portfolio'] })
      void queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      void queryClient.invalidateQueries({ queryKey: ['walletTransactions'] })
    } else if (status === 'cancelled') {
      toast.error('Checkout cancelled')
    }
  }, [searchParams, queryClient])

  return null
}

export default function WalletPage() {
  return (
    <div className="flex flex-col gap-6">
      <Suspense fallback={null}>
        <WalletStatusListener />
      </Suspense>

      <div>
        <h1 className="text-xl font-semibold text-[var(--color-text-primary)]">Wallet</h1>
        <p className="text-sm text-[var(--color-text-secondary)]">
          Top up your virtual trading balance via Razorpay (Test Mode).
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <TopUpForm />

        <div className="rounded-xl border border-[var(--color-border)] bg-[var(--color-card-bg)] p-6">
          <h2 className="text-lg font-semibold text-[var(--color-text-primary)]">
            Transaction history
          </h2>
          <div className="mt-4">
            <WalletTransactionsTable />
          </div>
        </div>
      </div>
    </div>
  )
}
