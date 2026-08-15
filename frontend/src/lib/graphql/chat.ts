export const GET_CHAT_HISTORY = `
  query GetChatHistory {
    chatHistory {
      id
      role
      content
      createdAt
    }
  }
`

export const GET_PENDING_ORDER = `
  query GetPendingOrder {
    pendingOrder {
      symbol
      side
      type
      quantity
      limitPrice
      stopPrice
      estimatedCost
    }
  }
`
