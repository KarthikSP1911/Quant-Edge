import { API_BASE_URL } from '@/lib/config'
import { getAccessToken } from '@/lib/auth/tokens'
import type { OrderFillEvent } from '@/types/order'

/**
 * Opens the order-fill SSE connection. EventSource can't set an Authorization header, so the
 * access token is passed as a query param — the backend only honors that on this one path.
 * Returns null if there's no token to authenticate with yet.
 */
export function connectOrderStream(onFill: (event: OrderFillEvent) => void): EventSource | null {
  const token = getAccessToken()
  if (!token) return null

  const source = new EventSource(
    `${API_BASE_URL}/api/orders/stream?token=${encodeURIComponent(token)}`,
  )

  source.addEventListener('order-fill', (event) => {
    const messageEvent = event as MessageEvent<string>
    onFill(JSON.parse(messageEvent.data) as OrderFillEvent)
  })

  return source
}
