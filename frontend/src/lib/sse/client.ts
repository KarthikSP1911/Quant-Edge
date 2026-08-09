import { API_BASE_URL } from '@/lib/config'
import { getAccessToken } from '@/lib/auth/tokens'

// EventSource can't send an Authorization header, so the access token is passed as a query
// param instead - the backend only honors that on a small set of SSE paths (JwtAuthFilter).
export const createSseConnection = (path: string) => {
  const token = getAccessToken()
  const query = token ? `?token=${encodeURIComponent(token)}` : ''
  return new EventSource(`${API_BASE_URL}${path}${query}`)
}
