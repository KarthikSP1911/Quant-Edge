import { useQuery } from '@tanstack/react-query'
import { graphqlRequest } from '@/lib/graphql/client'
import { GET_RESEARCH_NOTES } from '@/lib/graphql/research'

export interface ResearchNote {
  id: string
  company: {
    symbol: string
    name: string
  }
  title: string
  content: string
  generatedBy: string
  createdAt: string
}

export function useResearchNotes(symbol?: string) {
  return useQuery({
    queryKey: ['researchNotes', symbol],
    queryFn: async () => {
      const data = await graphqlRequest<{ researchNotes: ResearchNote[] }>(GET_RESEARCH_NOTES, {
        symbol,
      })
      return data.researchNotes
    },
  })
}
