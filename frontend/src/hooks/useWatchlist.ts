'use client'

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  addToWatchlist,
  fetchWatchlist,
  removeFromWatchlist,
  type WatchlistCompany,
} from '@/lib/graphql/watchlist'

const WATCHLIST_QUERY_KEY = ['watchlist']

export function useWatchlist() {
  return useQuery({
    queryKey: WATCHLIST_QUERY_KEY,
    queryFn: fetchWatchlist,
  })
}

export function useAddToWatchlist() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (symbol: string) => addToWatchlist(symbol),
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: WATCHLIST_QUERY_KEY })
    },
  })
}

export function useRemoveFromWatchlist() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (symbol: string) => removeFromWatchlist(symbol),
    onMutate: async (symbol: string) => {
      await queryClient.cancelQueries({ queryKey: WATCHLIST_QUERY_KEY })
      const previous = queryClient.getQueryData<WatchlistCompany[]>(WATCHLIST_QUERY_KEY)

      queryClient.setQueryData<WatchlistCompany[]>(WATCHLIST_QUERY_KEY, (current) =>
        current?.filter((company) => company.symbol !== symbol),
      )

      return { previous }
    },
    onError: (_err, _symbol, context) => {
      if (context?.previous) {
        queryClient.setQueryData(WATCHLIST_QUERY_KEY, context.previous)
      }
    },
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: WATCHLIST_QUERY_KEY })
    },
  })
}
