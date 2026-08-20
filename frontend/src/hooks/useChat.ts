import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { graphqlRequest } from '@/lib/graphql/client'
import { GET_CHAT_HISTORY, GET_PENDING_ORDER } from '@/lib/graphql/chat'
import {
  cancelPendingOrder,
  clearChatHistory,
  confirmPendingOrder,
  sendChatMessage,
} from '@/lib/api/chat'
import type { PendingOrder } from '@/types/order'

export interface ChatMessage {
  id: string
  role: 'USER' | 'ASSISTANT'
  content: string
  createdAt: string
}

const CHAT_HISTORY_KEY = ['chatHistory']
const PENDING_ORDER_KEY = ['pendingOrder']

export function useChatHistory() {
  return useQuery({
    queryKey: CHAT_HISTORY_KEY,
    queryFn: async () => {
      const data = await graphqlRequest<{ chatHistory: ChatMessage[] }>(GET_CHAT_HISTORY)
      return data.chatHistory
    },
  })
}

export function usePendingOrder() {
  return useQuery({
    queryKey: PENDING_ORDER_KEY,
    queryFn: async () => {
      const data = await graphqlRequest<{ pendingOrder: PendingOrder | null }>(GET_PENDING_ORDER)
      return data.pendingOrder
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
        id: `optimistic-${crypto.randomUUID()}`,
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
      void queryClient.invalidateQueries({ queryKey: PENDING_ORDER_KEY })
    },
  })
}

export function useConfirmPendingOrder() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: confirmPendingOrder,
    onSuccess: () => {
      queryClient.setQueryData<PendingOrder | null>(PENDING_ORDER_KEY, null)
      void queryClient.invalidateQueries({ queryKey: CHAT_HISTORY_KEY })
    },
  })
}

export function useCancelPendingOrder() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: cancelPendingOrder,
    onSuccess: () => {
      queryClient.setQueryData<PendingOrder | null>(PENDING_ORDER_KEY, null)
    },
  })
}

export function useClearChatHistory() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: clearChatHistory,
    onSuccess: () => {
      queryClient.setQueryData<ChatMessage[]>(CHAT_HISTORY_KEY, [])
      queryClient.setQueryData(PENDING_ORDER_KEY, null)
    },
  })
}
