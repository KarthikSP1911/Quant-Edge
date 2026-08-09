export const triggerResearch = async (symbol: string): Promise<{ sessionId: string }> => {
  const token = localStorage.getItem('token')
  const response = await fetch(`/api/v1/agent/research/${symbol}`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })

  if (!response.ok) {
    throw new Error('Failed to trigger research')
  }

  return response.json()
}
