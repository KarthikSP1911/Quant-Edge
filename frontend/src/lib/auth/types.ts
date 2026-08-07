export interface RegisterPayload {
  name: string
  email: string
  password: string
}

export interface LoginPayload {
  email: string
  password: string
}

export interface AccessTokenResponse {
  accessToken: string
}

export type UserRole = 'USER' | 'ADMIN'

export interface AuthUser {
  id: string
  email: string
  name: string
  role: UserRole
}
