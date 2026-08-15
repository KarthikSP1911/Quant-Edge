'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { useTopUpWallet } from '@/hooks/useTopUpWallet'
import { formatPrice } from '@/lib/utils/format'

const PRESET_AMOUNTS = [10, 25, 50, 100]

const formSchema = z.object({
  amountUsd: z.coerce.number<number>().min(1, 'Minimum top-up is $1.00'),
})

type FormValues = z.infer<typeof formSchema>

export default function TopUpForm() {
  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: { amountUsd: 25 },
  })

  const mutation = useTopUpWallet()
  const amountUsd = watch('amountUsd') || 0

  const onSubmit = (values: FormValues) => {
    mutation.mutate(values.amountUsd)
  }

  return (
    <form
      onSubmit={(e) => void handleSubmit(onSubmit)(e)}
      className="rounded-xl border border-[var(--color-border)] bg-[var(--color-card-bg)] p-6"
    >
      <h2 className="text-lg font-semibold text-[var(--color-text-primary)]">Top up wallet</h2>
      <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
        Pay with a card via Razorpay (Test Mode). No real charge will be made. Credits are added to
        your virtual trading balance at $1 = 10 credits.
      </p>

      <div className="mt-4 grid grid-cols-4 gap-2">
        {PRESET_AMOUNTS.map((preset) => (
          <button
            key={preset}
            type="button"
            onClick={() => setValue('amountUsd', preset, { shouldValidate: true })}
            className={`rounded-md border px-3 py-2 text-sm font-medium transition-colors ${
              amountUsd === preset
                ? 'border-[var(--color-accent-blue)] bg-[var(--color-accent-light)] text-[var(--color-accent-blue)]'
                : 'border-[var(--color-border)] text-[var(--color-text-primary)] hover:bg-[var(--color-sidebar-hover)]'
            }`}
          >
            ${preset}
          </button>
        ))}
      </div>

      <label className="mt-4 block text-xs font-medium text-[var(--color-text-secondary)]">
        Custom amount (USD)
      </label>
      <input
        type="number"
        min={1}
        step={1}
        {...register('amountUsd')}
        className="mt-1 w-full rounded-md border border-[var(--color-border)] px-3 py-2 text-sm tabular-nums text-[var(--color-text-primary)] focus:border-[var(--color-accent-blue)] focus:ring-1 focus:ring-[var(--color-accent-blue)] focus:outline-none"
      />
      {errors.amountUsd && (
        <p className="mt-1 text-xs text-[var(--color-loss)]">{errors.amountUsd.message}</p>
      )}

      <div className="mt-4 flex items-center justify-between text-sm">
        <span className="text-[var(--color-text-secondary)]">You&apos;ll receive</span>
        <span className="font-semibold tabular-nums text-[var(--color-profit)]">
          {formatPrice(amountUsd * 10)} credits
        </span>
      </div>

      {mutation.isError && (
        <p className="mt-3 text-xs text-[var(--color-loss)]">{mutation.error.message}</p>
      )}

      <button
        type="submit"
        disabled={amountUsd < 1 || mutation.isPending}
        className="mt-5 w-full rounded-md bg-[var(--color-accent-blue)] px-4 py-2 text-sm font-medium text-white transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
      >
        {mutation.isPending ? 'Opening Razorpay…' : `Pay ${formatPrice(amountUsd)}`}
      </button>
    </form>
  )
}
