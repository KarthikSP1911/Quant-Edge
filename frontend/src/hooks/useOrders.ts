'use client'

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { cancelOrder } from '@/lib/api/orders'
import { fetchFilledOrders, fetchOpenOrders, fetchOrderHistory } from '@/lib/graphql/orders'

const ORDERS_QUERY_KEY = ['orders']

export function useOpenOrders() {
  return useQuery({ queryKey: [...ORDERS_QUERY_KEY, 'open'], queryFn: fetchOpenOrders })
}

export function useFilledOrders() {
  return useQuery({ queryKey: [...ORDERS_QUERY_KEY, 'filled'], queryFn: fetchFilledOrders })
}

export function useOrderHistory() {
  return useQuery({ queryKey: [...ORDERS_QUERY_KEY, 'history'], queryFn: fetchOrderHistory })
}

export function useCancelOrder() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (orderId: string) => cancelOrder(orderId),
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: ORDERS_QUERY_KEY })
    },
  })
}
