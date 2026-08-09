import { API_BASE_URL } from '@/lib/config'
import { getAccessToken } from '@/lib/auth/tokens'

export interface ChatSendResponse {
  response: string
}

export async function sendChatMessage(message: string): Promise<ChatSendResponse> {
  const token = getAccessToken()
  const response = await fetch(`${API_BASE_URL}/api/v1/chat`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ message }),
  })

  if (!response.ok) {
    throw new Error('Failed to send chat message')
  }

  return response.json()
}
