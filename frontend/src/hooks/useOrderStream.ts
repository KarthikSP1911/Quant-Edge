'use client'

import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { connectOrderStream } from '@/lib/sse/orderStream'
import { getAccessToken } from '@/lib/auth/tokens'
import { formatPrice } from '@/lib/utils/format'
import type { OrderFillEvent } from '@/types/order'

const RECONNECT_DELAY_MS = 3000

/** Subscribes to the order-fill SSE stream for as long as it's mounted: toasts fills and
 * invalidates the caches they affect. Reconnects on drop with a fixed backoff. */
export function useOrderStream() {
  const queryClient = useQueryClient()

  useEffect(() => {
    let source: EventSource | null = null
    let reconnectTimer: ReturnType<typeof setTimeout> | null = null
    let stopped = false

    const handleFill = (fill: OrderFillEvent) => {
      const verb = fill.side === 'BUY' ? 'Bought' : 'Sold'
      toast.success(`${verb} ${fill.quantity} ${fill.symbol} @ ${formatPrice(fill.price)}`)
      void queryClient.invalidateQueries({ queryKey: ['orders'] })
      void queryClient.invalidateQueries({ queryKey: ['portfolio'] })
      void queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    }

    const connect = () => {
      if (stopped || !getAccessToken()) return
      source = connectOrderStream(handleFill)
      if (!source) return

      source.onerror = () => {
        source?.close()
        source = null
        if (!stopped) {
          reconnectTimer = setTimeout(connect, RECONNECT_DELAY_MS)
        }
      }
    }

    connect()

    return () => {
      stopped = true
      source?.close()
      if (reconnectTimer) clearTimeout(reconnectTimer)
    }
  }, [queryClient])
}
