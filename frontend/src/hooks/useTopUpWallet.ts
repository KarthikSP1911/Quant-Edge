'use client'

import { useMutation } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { createCheckoutSession, verifyPayment } from '@/lib/api/wallet'
import { loadRazorpayScript } from '@/lib/razorpay/loadRazorpayScript'
import type { RazorpayPaymentResponse } from '@/types/razorpay'

const RAZORPAY_KEY_ID = process.env.NEXT_PUBLIC_RAZORPAY_KEY_ID ?? ''

export function useTopUpWallet() {
  const router = useRouter()

  return useMutation<void, Error, number>({
    mutationFn: async (amountUsd) => {
      const order = await createCheckoutSession(amountUsd)
      await loadRazorpayScript()

      return new Promise<void>((resolve, reject) => {
        const checkout = new window.Razorpay({
          key: RAZORPAY_KEY_ID,
          amount: order.amount,
          currency: order.currency,
          order_id: order.orderId,
          name: 'QuantEdge',
          theme: { color: '#2563EB' },
          handler: (response: RazorpayPaymentResponse) => {
            verifyPayment({
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            })
              .then(() => {
                router.replace('/wallet?status=success')
                resolve()
              })
              .catch(reject)
          },
          modal: {
            ondismiss: () => {
              router.replace('/wallet?status=cancelled')
              resolve()
            },
          },
        })
        checkout.open()
      })
    },
  })
}
