export const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'

// Things that must reach the backend directly instead of through the Next.js proxy:
//  - SSE: proxies can buffer long-lived streams. It authenticates with a query-string token
//    (EventSource can't send headers and no cookie is involved), so cross-origin is fine as long
//    as the backend's CORS allows this origin.
//  - Google OAuth2: Spring keeps the login state in a server session cookie, so the flow has to
//    start and finish on the same (backend) host - the callback lands there too.
// Falls back to the API base when unset.
export const DIRECT_BACKEND_URL = process.env.NEXT_PUBLIC_DIRECT_BACKEND_URL ?? API_BASE_URL
