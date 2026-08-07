import { API_BASE_URL } from '@/lib/config'
import { getAccessToken } from '@/lib/auth/tokens'

export class GraphQLError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'GraphQLError'
  }
}

interface GraphQLResponse<T> {
  data?: T
  errors?: { message: string }[]
}

export async function graphqlRequest<T>(
  query: string,
  variables?: Record<string, unknown>,
): Promise<T> {
  const token = getAccessToken()
  const response = await fetch(`${API_BASE_URL}/graphql`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ query, variables }),
  })

  if (!response.ok) {
    throw new GraphQLError(`GraphQL request failed with status ${response.status}`)
  }

  const json = (await response.json()) as GraphQLResponse<T>
  if (json.errors && json.errors.length > 0) {
    throw new GraphQLError(json.errors.map((error) => error.message).join(', '))
  }
  if (!json.data) {
    throw new GraphQLError('GraphQL response missing data')
  }
  return json.data
}
