import { refreshAccessToken } from './api'
import { clearAccessToken, setAccessToken } from './tokens'

let refreshPromise: Promise<string | null> | null = null

// The access token is short-lived (15 min - see backend jwt.access-expiration-ms), but nothing
// ever called this endpoint before, so every request made after that window returned 401 with no
// automatic recovery. Callers that hit a 401 should call this once and retry; concurrent 401s
// from parallel requests share the same in-flight refresh instead of each firing their own.
export function refreshTokenOnce(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = refreshAccessToken()
      .then(({ accessToken }) => {
        setAccessToken(accessToken)
        return accessToken
      })
      .catch(() => {
        clearAccessToken()
        if (typeof window !== 'undefined') {
          // Plain module outside the component tree — no router instance to push() with, and a
          // full reload is fine here since the session is dead anyway.
          // eslint-disable-next-line @next/next/no-location-assign-relative-destination
          window.location.href = '/login'
        }
        return null
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}
