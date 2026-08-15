import type { WalletTransaction } from '@/types/wallet'
import { graphqlRequest } from './client'

const WALLET_TRANSACTIONS_QUERY = /* GraphQL */ `
  query WalletTransactions {
    walletTransactions {
      id
      amountUsdCents
      creditsAwarded
      status
      createdAt
    }
  }
`

export async function fetchWalletTransactions(): Promise<WalletTransaction[]> {
  const data = await graphqlRequest<{ walletTransactions: WalletTransaction[] }>(
    WALLET_TRANSACTIONS_QUERY,
  )
  return data.walletTransactions
}
