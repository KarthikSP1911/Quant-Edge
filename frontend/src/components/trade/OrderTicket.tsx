'use client'

import { useEffect } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { usePlaceOrder } from '@/hooks/usePlaceOrder'
import { usePortfolio } from '@/hooks/usePortfolio'
import { useTradeOrder } from '@/hooks/useTradeOrder'
import { formatPrice } from '@/lib/utils/format'
import type { OrderSide, OrderType } from '@/types/order'

const orderTypes: { value: OrderType; label: string }[] = [
  { value: 'MARKET', label: 'Market' },
  { value: 'LIMIT', label: 'Limit' },
  { value: 'STOP_LOSS', label: 'Stop-Loss' },
  { value: 'STOP_LIMIT', label: 'Stop-Limit' },
]

const formSchema = z
  .object({
    type: z.enum(['MARKET', 'LIMIT', 'STOP_LOSS', 'STOP_LIMIT']),
    quantity: z.coerce.number<number>().int().min(1, 'Quantity must be at least 1'),
    limitPrice: z.coerce.number<number>().optional(),
    stopPrice: z.coerce.number<number>().optional(),
    timeInForce: z.enum(['DAY', 'GTC']),
  })
  .superRefine((data, ctx) => {
    const needsLimit = data.type === 'LIMIT' || data.type === 'STOP_LIMIT'
    const needsStop = data.type === 'STOP_LOSS' || data.type === 'STOP_LIMIT'

    if (needsLimit && !(data.limitPrice && data.limitPrice > 0)) {
      ctx.addIssue({ code: 'custom', path: ['limitPrice'], message: 'Limit price is required' })
    }
    if (needsStop && !(data.stopPrice && data.stopPrice > 0)) {
      ctx.addIssue({ code: 'custom', path: ['stopPrice'], message: 'Stop price is required' })
    }
  })

type FormValues = z.infer<typeof formSchema>

export default function OrderTicket({
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
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: { type: 'MARKET', quantity: 1, timeInForce: 'GTC' },
  })

  const marketMutation = useTradeOrder()
  const limitMutation = usePlaceOrder()
  const { data: portfolio } = usePortfolio()

  const type = watch('type')
  const quantity = watch('quantity') || 0
  const isBuy = side === 'BUY'
  const isMarket = type === 'MARKET'
  const showLimitPrice = type === 'LIMIT' || type === 'STOP_LIMIT'
  const showStopPrice = type === 'STOP_LOSS' || type === 'STOP_LIMIT'

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [onClose])

  const cashBalance = portfolio?.summary.cashBalance ?? 0
  const ownedQuantity = portfolio?.holdings.find((h) => h.symbol === symbol)?.quantity ?? 0
  const estimatedTotal = quantity * price
  const overLimit = isMarket && (isBuy ? estimatedTotal > cashBalance : quantity > ownedQuantity)

  const mutation = isMarket ? marketMutation : limitMutation
  const canSubmit = quantity > 0 && !overLimit && !mutation.isPending

  const onSubmit = (values: FormValues) => {
    if (values.type === 'MARKET') {
      marketMutation.mutate({ symbol, side, quantity: values.quantity })
      return
    }
    limitMutation.mutate({
      symbol,
      side,
      type: values.type,
      quantity: values.quantity,
      limitPrice: showLimitPrice ? (values.limitPrice ?? null) : null,
      stopPrice: showStopPrice ? (values.stopPrice ?? null) : null,
      timeInForce: values.timeInForce,
    })
  }

  if (marketMutation.isSuccess) {
    const result = marketMutation.data
    return (
      <ModalShell onClose={onClose}>
        <p className="text-sm font-semibold text-[var(--color-profit)]">Order filled</p>
        <p className="mt-2 text-sm text-[var(--color-text-secondary)]">
          {isBuy ? 'Bought' : 'Sold'} {result.quantity} sh {symbol} @{' '}
          {formatPrice(result.executionPrice)} &middot; total{' '}
          {formatPrice(result.quantity * result.executionPrice)}
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

  if (limitMutation.isSuccess) {
    const result = limitMutation.data
    return (
      <ModalShell onClose={onClose}>
        <p className="text-sm font-semibold text-[var(--color-profit)]">Order placed</p>
        <p className="mt-2 text-sm text-[var(--color-text-secondary)]">
          {isBuy ? 'Buy' : 'Sell'} {result.quantity} sh {symbol} resting as{' '}
          {result.type.replace('_', ' ').toLowerCase()}, status {result.status}
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

      <form onSubmit={(e) => void handleSubmit(onSubmit)(e)}>
        <label className="mt-4 block text-xs font-medium text-[var(--color-text-secondary)]">
          Order type
        </label>
        <select
          {...register('type')}
          className="mt-1 w-full rounded-md border border-[var(--color-border)] px-3 py-2 text-sm text-[var(--color-text-primary)] focus:border-[var(--color-accent-blue)] focus:outline-none"
        >
          {orderTypes.map((t) => (
            <option key={t.value} value={t.value}>
              {t.label}
            </option>
          ))}
        </select>

        <label className="mt-4 block text-xs font-medium text-[var(--color-text-secondary)]">
          Quantity
        </label>
        <input
          type="number"
          min={1}
          step={1}
          autoFocus
          {...register('quantity')}
          className="mt-1 w-full rounded-md border border-[var(--color-border)] px-3 py-2 text-sm tabular-nums text-[var(--color-text-primary)] focus:border-[var(--color-accent-blue)] focus:outline-none"
        />
        {errors.quantity && (
          <p className="mt-1 text-xs text-[var(--color-loss)]">{errors.quantity.message}</p>
        )}

        {showLimitPrice && (
          <>
            <label className="mt-4 block text-xs font-medium text-[var(--color-text-secondary)]">
              Limit price
            </label>
            <input
              type="number"
              min={0.01}
              step={0.01}
              {...register('limitPrice')}
              className="mt-1 w-full rounded-md border border-[var(--color-border)] px-3 py-2 text-sm tabular-nums text-[var(--color-text-primary)] focus:border-[var(--color-accent-blue)] focus:outline-none"
            />
            {errors.limitPrice && (
              <p className="mt-1 text-xs text-[var(--color-loss)]">{errors.limitPrice.message}</p>
            )}
          </>
        )}

        {showStopPrice && (
          <>
            <label className="mt-4 block text-xs font-medium text-[var(--color-text-secondary)]">
              Stop price
            </label>
            <input
              type="number"
              min={0.01}
              step={0.01}
              {...register('stopPrice')}
              className="mt-1 w-full rounded-md border border-[var(--color-border)] px-3 py-2 text-sm tabular-nums text-[var(--color-text-primary)] focus:border-[var(--color-accent-blue)] focus:outline-none"
            />
            {errors.stopPrice && (
              <p className="mt-1 text-xs text-[var(--color-loss)]">{errors.stopPrice.message}</p>
            )}
          </>
        )}

        {!isMarket && (
          <>
            <label className="mt-4 block text-xs font-medium text-[var(--color-text-secondary)]">
              Time in force
            </label>
            <select
              {...register('timeInForce')}
              className="mt-1 w-full rounded-md border border-[var(--color-border)] px-3 py-2 text-sm text-[var(--color-text-primary)] focus:border-[var(--color-accent-blue)] focus:outline-none"
            >
              <option value="GTC">Good till cancelled</option>
              <option value="DAY">Day only</option>
            </select>
          </>
        )}

        {isMarket && (
          <>
            <div className="mt-4 flex items-center justify-between text-sm">
              <span className="text-[var(--color-text-secondary)]">Estimated total</span>
              <span className="font-semibold tabular-nums text-[var(--color-text-primary)]">
                {formatPrice(estimatedTotal)}
              </span>
            </div>
            <div className="mt-1 flex items-center justify-between text-xs text-[var(--color-text-muted)]">
              <span>{isBuy ? 'Cash available' : 'Shares owned'}</span>
              <span className="tabular-nums">
                {isBuy ? formatPrice(cashBalance) : `${ownedQuantity} sh`}
              </span>
            </div>
          </>
        )}
        {!isMarket && (
          <p className="mt-4 text-xs text-[var(--color-text-muted)]">
            Fills use 15-min synced prices, not real-time ticks, and rest on the book until
            triggered.
          </p>
        )}

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
            type="submit"
            disabled={!canSubmit}
            className={`flex-1 rounded-md px-4 py-2 text-sm font-medium text-white ${
              isBuy ? 'bg-[var(--color-profit)]' : 'bg-[var(--color-loss)]'
            } disabled:opacity-50`}
          >
            {mutation.isPending
              ? 'Placing order…'
              : `${isBuy ? 'Buy' : 'Sell'} ${quantity || 0} sh`}
          </button>
        </div>
      </form>
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
