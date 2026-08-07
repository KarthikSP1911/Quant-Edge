'use client'

import { useMutation } from '@tanstack/react-query'
import { placeOrder } from '@/lib/api/orders'
import type { TradeOrderInput, TradeOrderResult } from '@/types/order'

export function useTradeOrder(price: number) {
  return useMutation<TradeOrderResult, Error, TradeOrderInput>({
    mutationFn: (input) => placeOrder(input, price),
  })
}
