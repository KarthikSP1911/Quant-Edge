import type { Order } from '@/types/order'
import { graphqlRequest } from './client'

const ORDER_FIELDS = `
  id
  symbol
  side
  type
  status
  quantity
  filledQuantity
  limitPrice
  stopPrice
  createdAt
  updatedAt
  expiresAt
`

const OPEN_ORDERS_QUERY = /* GraphQL */ `
  query OpenOrders {
    openOrders {
      ${ORDER_FIELDS}
    }
  }
`

const FILLED_ORDERS_QUERY = /* GraphQL */ `
  query FilledOrders {
    filledOrders {
      ${ORDER_FIELDS}
    }
  }
`

const ORDER_HISTORY_QUERY = /* GraphQL */ `
  query OrderHistory {
    orderHistory {
      ${ORDER_FIELDS}
    }
  }
`

export async function fetchOpenOrders(): Promise<Order[]> {
  const data = await graphqlRequest<{ openOrders: Order[] }>(OPEN_ORDERS_QUERY)
  return data.openOrders
}

export async function fetchFilledOrders(): Promise<Order[]> {
  const data = await graphqlRequest<{ filledOrders: Order[] }>(FILLED_ORDERS_QUERY)
  return data.filledOrders
}

export async function fetchOrderHistory(): Promise<Order[]> {
  const data = await graphqlRequest<{ orderHistory: Order[] }>(ORDER_HISTORY_QUERY)
  return data.orderHistory
}
