import { API_BASE_URL } from '@/lib/config'
import { getAccessToken } from '@/lib/auth/tokens'

export const triggerResearch = async (symbol: string): Promise<{ sessionId: string }> => {
  const token = getAccessToken()
  const response = await fetch(`${API_BASE_URL}/api/v1/agent/research/${symbol}`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })

  if (!response.ok) {
    throw new Error('Failed to trigger research')
  }

  return response.json()
}
