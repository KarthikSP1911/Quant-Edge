'use client'

import { useWalletTransactions } from '@/hooks/useWalletTransactions'
import { formatPrice } from '@/lib/utils/format'

export default function WalletTransactionsTable() {
  const { data, isPending, isError } = useWalletTransactions()

  if (isPending) {
    return <p className="text-sm text-[var(--color-text-secondary)]">Loading transactions…</p>
  }

  if (isError) {
    return (
      <p className="text-sm text-[var(--color-loss)]">Couldn&apos;t load transaction history.</p>
    )
  }

  if (data.length === 0) {
    return <p className="text-sm text-[var(--color-text-secondary)]">No top-ups yet.</p>
  }

  return (
    <table className="w-full text-sm">
      <thead>
        <tr className="border-b border-[var(--color-border)] text-left text-xs text-[var(--color-text-secondary)]">
          <th className="pb-2 font-medium">Date</th>
          <th className="pb-2 font-medium">Amount paid</th>
          <th className="pb-2 font-medium">Credits</th>
          <th className="pb-2 font-medium">Status</th>
        </tr>
      </thead>
      <tbody>
        {data.map((transaction) => (
          <tr key={transaction.id} className="border-b border-[var(--color-border)] last:border-0">
            <td className="py-2 text-[var(--color-text-secondary)]">
              {new Date(transaction.createdAt).toLocaleString()}
            </td>
            <td className="py-2 tabular-nums text-[var(--color-text-primary)]">
              {formatPrice(transaction.amountUsdCents / 100)}
            </td>
            <td className="py-2 tabular-nums text-[var(--color-profit)]">
              {formatPrice(transaction.creditsAwarded)}
            </td>
            <td className="py-2 text-[var(--color-text-secondary)]">{transaction.status}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
