'use client'

import { useMutation, useQueryClient } from '@tanstack/react-query'
import { placeOrder } from '@/lib/api/orders'
import type { TradeOrderInput, TradeOrderResult } from '@/types/order'

export function useTradeOrder() {
  const queryClient = useQueryClient()

  return useMutation<TradeOrderResult, Error, TradeOrderInput>({
    mutationFn: placeOrder,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['portfolio'] })
      void queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}
