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

export async function clearChatHistory(): Promise<void> {
  const token = getAccessToken()
  const response = await fetch(`${API_BASE_URL}/api/v1/chat`, {
    method: 'DELETE',
    credentials: 'include',
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })

  if (!response.ok) {
    throw new Error('Failed to clear chat history')
  }
}

// Deterministic authorization for a chat-agent-staged trade: the LLM can only propose an order
// (see ChatTools#placeOrder); these are the only endpoints that ever execute or discard it, and
// they're called exclusively by the user's own click on the confirmation card, never by a chat
// message the model could interpret on its own.
export async function confirmPendingOrder(): Promise<unknown> {
  const token = getAccessToken()
  const response = await fetch(`${API_BASE_URL}/api/v1/chat/pending-order/confirm`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })

  if (!response.ok) {
    throw new Error('Failed to confirm pending order')
  }

  return response.json()
}

export async function cancelPendingOrder(): Promise<void> {
  const token = getAccessToken()
  const response = await fetch(`${API_BASE_URL}/api/v1/chat/pending-order/cancel`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })

  if (!response.ok) {
    throw new Error('Failed to cancel pending order')
  }
}
