export const createSseConnection = (url: string) => {
  // If we need auth tokens, we might need EventSourcePolyfill, but EventSource doesn't support headers natively.
  // Wait, standard EventSource doesn't send auth headers, it uses cookies. We might need to use fetch if we need Bearer tokens.
  // Actually, we can use the fetch API with stream reading, or event-source-polyfill.
  // For simplicity, assuming the backend doesn't strictly need JWT for SSE trace if it's protected by CORS/cookies, or we pass token in URL.
  // Let's pass the token in URL query parameter `?token=...` if necessary, or just standard EventSource.
  // The backend controller for trace: `@GetMapping("/trace/{sessionId}") public SseEmitter connectTrace(...)`.
  // Wait, does it have `@AuthenticationPrincipal User user`? Let's check `ResearchAgentController.java`.
  // `connectTrace` doesn't have `@AuthenticationPrincipal User user`. So it doesn't need auth! Just the sessionId!
  return new EventSource(url)
}
