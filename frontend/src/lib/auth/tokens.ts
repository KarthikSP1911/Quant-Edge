// The refresh token is no longer handled here: the backend sets it as an httpOnly cookie
// (see AuthController), so client-side JS never has access to it.

export const setAccessToken = (accessToken: string) => {
  if (typeof window !== 'undefined') {
    localStorage.setItem('accessToken', accessToken)
  }
}

export const getAccessToken = () => {
  if (typeof window !== 'undefined') {
    return localStorage.getItem('accessToken')
  }
  return null
}

export const clearAccessToken = () => {
  if (typeof window !== 'undefined') {
    localStorage.removeItem('accessToken')
  }
}
