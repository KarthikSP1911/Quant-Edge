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
