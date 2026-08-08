'use client'

import { useMutation, useQueryClient } from '@tanstack/react-query'
import { placeLimitOrder } from '@/lib/api/orders'
import type { PlaceOrderInput, PlacedOrderResult } from '@/types/order'

export function usePlaceOrder() {
  const queryClient = useQueryClient()

  return useMutation<PlacedOrderResult, Error, PlaceOrderInput>({
    mutationFn: placeLimitOrder,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['portfolio'] })
      void queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}
