// Mirrors the backend's RazorpayOrderResponse (POST /api/wallet/checkout-session).
export interface RazorpayOrderResult {
  orderId: string
  amount: number
  currency: string
}

// Fields Razorpay's Checkout.js handler callback returns, forwarded to POST /api/wallet/verify-payment.
export interface VerifyPaymentPayload {
  razorpayOrderId: string
  razorpayPaymentId: string
  razorpaySignature: string
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
