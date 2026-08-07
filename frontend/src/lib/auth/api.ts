import { API_BASE_URL } from '@/lib/config'
import type { AccessTokenResponse, AuthUser, LoginPayload, RegisterPayload } from './types'

export class ApiError extends Error {
  status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

// The backend returns two different error shapes: {"error": "message"} for
// ApiException/AuthenticationException, or a flat field->message map for
// bean-validation failures (see GlobalExceptionHandler). Handle both.
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

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
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

export function register(payload: RegisterPayload) {
  return request<AccessTokenResponse>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function login(payload: LoginPayload) {
  return request<AccessTokenResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function refreshAccessToken() {
  return request<AccessTokenResponse>('/api/auth/refresh', { method: 'POST' })
}

export function logout() {
  return request<void>('/api/auth/logout', { method: 'POST' })
}

export function exchangeOAuthCode(code: string) {
  return request<AccessTokenResponse>('/api/auth/oauth2/callback', {
    method: 'POST',
    body: JSON.stringify({ code }),
  })
}

export function getCurrentUser(accessToken: string) {
  return request<AuthUser>('/api/auth/me', {
    headers: { Authorization: `Bearer ${accessToken}` },
  })
}

export function googleLoginUrl() {
  return `${API_BASE_URL}/oauth2/authorization/google`
}
