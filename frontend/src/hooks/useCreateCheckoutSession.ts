'use client'

import { useMutation } from '@tanstack/react-query'
import { createCheckoutSession } from '@/lib/api/wallet'
import type { CheckoutSessionResult } from '@/types/wallet'

export function useCreateCheckoutSession() {
  return useMutation<CheckoutSessionResult, Error, number>({
    mutationFn: createCheckoutSession,
    onSuccess: (data) => {
      window.location.href = data.checkoutUrl
    },
  })
}
