import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { graphqlRequest } from '@/lib/graphql/client'
import { GET_CHAT_HISTORY } from '@/lib/graphql/chat'
import { sendChatMessage } from '@/lib/api/chat'

export interface ChatMessage {
  id: string
  role: 'USER' | 'ASSISTANT'
  content: string
  createdAt: string
}

const CHAT_HISTORY_KEY = ['chatHistory']

export function useChatHistory() {
  return useQuery({
    queryKey: CHAT_HISTORY_KEY,
    queryFn: async () => {
      const data = await graphqlRequest<{ chatHistory: ChatMessage[] }>(GET_CHAT_HISTORY)
      return data.chatHistory
    },
  })
}

export function useSendChatMessage() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (message: string) => sendChatMessage(message),
    onMutate: async (message: string) => {
      await queryClient.cancelQueries({ queryKey: CHAT_HISTORY_KEY })
      const previous = queryClient.getQueryData<ChatMessage[]>(CHAT_HISTORY_KEY)

      const optimisticUserMessage: ChatMessage = {
        id: `optimistic-${Date.now()}`,
        role: 'USER',
        content: message,
        createdAt: new Date().toISOString(),
      }
      queryClient.setQueryData<ChatMessage[]>(CHAT_HISTORY_KEY, (old) => [
        ...(old ?? []),
        optimisticUserMessage,
      ])

      return { previous }
    },
    onError: (_err, _message, context) => {
      if (context?.previous) {
        queryClient.setQueryData(CHAT_HISTORY_KEY, context.previous)
      }
    },
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: CHAT_HISTORY_KEY })
    },
  })
}
