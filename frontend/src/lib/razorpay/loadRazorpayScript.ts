let razorpayScriptPromise: Promise<void> | null = null

// Razorpay Checkout is a client-side modal loaded from their CDN — there's no npm package for it,
// so the script tag is injected once and cached (repeat top-ups reuse the same window.Razorpay).
export function loadRazorpayScript(): Promise<void> {
  if (typeof window !== 'undefined' && window.Razorpay) {
    return Promise.resolve()
  }
  if (!razorpayScriptPromise) {
    razorpayScriptPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = 'https://checkout.razorpay.com/v1/checkout.js'
      script.async = true
      script.onload = () => resolve()
      script.onerror = () => reject(new Error('Failed to load the Razorpay checkout script'))
      document.body.appendChild(script)
    })
  }
  return razorpayScriptPromise
}
