import { API_BASE_URL } from '@/lib/config'
import { getAccessToken } from '@/lib/auth/tokens'

export class ApiError extends Error {
  status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

// Mirrors lib/auth/api.ts's error handling — the backend returns either {"error": "message"}
// (ApiException/AuthenticationException) or a flat field->message map (bean validation).
async function parseErrorMessage(response: Response): Promise<string> {
  try {
    const data: unknown = await response.json()
    if (data && typeof data === 'object') {
      const record = data as Record<string, unknown>
      if (typeof record.error === 'string') {
        return record.error
      }
      const fieldMessages = Object.values(record).filter(
        (value): value is string => typeof value === 'string',
      )
      if (fieldMessages.length > 0) {
        return fieldMessages.join(', ')
      }
    }
  } catch {
    // response body wasn't JSON - fall through to the generic message below
  }
  return 'Something went wrong. Please try again.'
}

export async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getAccessToken()
  const response = await fetch(`${API_BASE_URL}${path}`, {
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init?.headers,
    },
    ...init,
  })

  if (!response.ok) {
    throw new ApiError(await parseErrorMessage(response), response.status)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}
