'use client'

import { useQuery } from '@tanstack/react-query'
import { fetchWalletTransactions } from '@/lib/graphql/wallet'

export function useWalletTransactions() {
  return useQuery({
    queryKey: ['walletTransactions'],
    queryFn: fetchWalletTransactions,
  })
}
