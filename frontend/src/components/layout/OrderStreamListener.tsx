'use client'

import { useOrderStream } from '@/hooks/useOrderStream'

/** Renders nothing — just keeps the order-fill SSE subscription alive while mounted. */
export default function OrderStreamListener() {
  useOrderStream()
  return null
}
