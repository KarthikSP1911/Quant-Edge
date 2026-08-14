// Mirrors the backend's CheckoutSessionResponse (POST /api/wallet/checkout-session).
export interface CheckoutSessionResult {
  checkoutUrl: string
  sessionId: string
}

export type WalletTransactionStatus = 'PENDING' | 'COMPLETED' | 'FAILED'

// Mirrors the backend's GraphQL WalletTransaction type (walletTransactions).
export interface WalletTransaction {
  id: string
  amountUsdCents: number
  creditsAwarded: number
  status: WalletTransactionStatus
  createdAt: string
}
