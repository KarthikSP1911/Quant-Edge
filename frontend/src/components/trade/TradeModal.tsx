'use client'

import { useEffect, useState } from 'react'
import { useTradeOrder } from '@/hooks/useTradeOrder'
import { getMockCashBalance, getMockOwnedQuantity } from '@/lib/api/orders'
import { formatPrice } from '@/lib/utils/format'
import type { OrderSide } from '@/types/order'

export default function TradeModal({
  symbol,
  side,
  price,
  onClose,
}: {
  symbol: string
  side: OrderSide
  price: number
  onClose: () => void
}) {
  const [quantity, setQuantity] = useState(1)
  const mutation = useTradeOrder(price)

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [onClose])

  const totalValue = quantity * price
  const cashBalance = getMockCashBalance()
  const ownedQuantity = getMockOwnedQuantity(symbol)
  const isBuy = side === 'BUY'

  const overLimit = isBuy ? totalValue > cashBalance : quantity > ownedQuantity
  const canSubmit = quantity > 0 && !overLimit && !mutation.isPending

  if (mutation.isSuccess) {
    return (
      <ModalShell onClose={onClose}>
        <p className="text-sm font-semibold text-[var(--color-profit)]">Order filled</p>
        <p className="mt-2 text-sm text-[var(--color-text-secondary)]">
          {isBuy ? 'Bought' : 'Sold'} {mutation.data.quantity} sh {symbol} @{' '}
          {formatPrice(mutation.data.price)} &middot; total {formatPrice(mutation.data.totalValue)}
        </p>
        <button
          type="button"
          onClick={onClose}
          className="mt-4 w-full rounded-md bg-[var(--color-accent-blue)] px-4 py-2 text-sm font-medium text-white"
        >
          Done
        </button>
      </ModalShell>
    )
  }

  return (
    <ModalShell onClose={onClose}>
      <h2 className="text-lg font-semibold text-[var(--color-text-primary)]">
        {isBuy ? 'Buy' : 'Sell'} {symbol}
      </h2>
      <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
        Market price {formatPrice(price)}
      </p>

      <label className="mt-4 block text-xs font-medium text-[var(--color-text-secondary)]">
        Quantity
      </label>
      <input
        type="number"
        min={1}
        step={1}
        autoFocus
        value={quantity}
        onChange={(e) => setQuantity(Math.max(0, Math.floor(Number(e.target.value))))}
        className="mt-1 w-full rounded-md border border-[var(--color-border)] px-3 py-2 text-sm tabular-nums text-[var(--color-text-primary)] focus:border-[var(--color-accent-blue)] focus:outline-none"
      />

      <div className="mt-4 flex items-center justify-between text-sm">
        <span className="text-[var(--color-text-secondary)]">Estimated total</span>
        <span className="font-semibold tabular-nums text-[var(--color-text-primary)]">
          {formatPrice(totalValue)}
        </span>
      </div>
      <div className="mt-1 flex items-center justify-between text-xs text-[var(--color-text-muted)]">
        <span>{isBuy ? 'Cash available' : 'Shares owned'}</span>
        <span className="tabular-nums">
          {isBuy ? formatPrice(cashBalance) : `${ownedQuantity} sh`}
        </span>
      </div>

      {overLimit && (
        <p className="mt-3 text-xs text-[var(--color-loss)]">
          {isBuy ? 'Order exceeds available cash.' : 'Order exceeds shares owned.'}
        </p>
      )}
      {mutation.isError && (
        <p className="mt-3 text-xs text-[var(--color-loss)]">{mutation.error.message}</p>
      )}

      <div className="mt-5 flex gap-2">
        <button
          type="button"
          onClick={onClose}
          className="flex-1 rounded-md border border-[var(--color-border)] px-4 py-2 text-sm font-medium text-[var(--color-text-primary)] hover:bg-[var(--color-sidebar-hover)]"
        >
          Cancel
        </button>
        <button
          type="button"
          disabled={!canSubmit}
          onClick={() => mutation.mutate({ symbol, side, quantity })}
          className={`flex-1 rounded-md px-4 py-2 text-sm font-medium text-white ${
            isBuy ? 'bg-[var(--color-profit)]' : 'bg-[var(--color-loss)]'
          } disabled:opacity-50`}
        >
          {mutation.isPending ? 'Placing order…' : `${isBuy ? 'Buy' : 'Sell'} ${quantity || 0} sh`}
        </button>
      </div>
    </ModalShell>
  )
}

function ModalShell({ children, onClose }: { children: React.ReactNode; onClose: () => void }) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4"
      onClick={onClose}
    >
      <div
        role="dialog"
        aria-modal="true"
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-sm rounded-lg bg-white p-6 shadow-lg"
      >
        {children}
      </div>
    </div>
  )
}
