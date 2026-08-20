const WEEKDAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri']

const MARKET_OPEN_MINUTES = 9 * 60 + 30 // 9:30 AM ET
const MARKET_CLOSE_MINUTES = 16 * 60 // 4:00 PM ET

export interface MarketStatus {
  isOpen: boolean
  message: string
}

function easternParts(date: Date): { weekday: string; hour: number; minute: number } {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: 'America/New_York',
    hour12: false,
    weekday: 'short',
    hour: '2-digit',
    minute: '2-digit',
  }).formatToParts(date)

  const get = (type: string) => parts.find((p) => p.type === type)?.value ?? ''

  return {
    weekday: get('weekday'),
    hour: Number(get('hour')),
    minute: Number(get('minute')),
  }
}

export function getMarketStatus(now: Date = new Date()): MarketStatus {
  const { weekday, hour, minute } = easternParts(now)
  const minutesSinceMidnight = hour * 60 + minute
  const isTradingDay = WEEKDAYS.includes(weekday)
  const isOpen =
    isTradingDay &&
    minutesSinceMidnight >= MARKET_OPEN_MINUTES &&
    minutesSinceMidnight < MARKET_CLOSE_MINUTES

  return {
    isOpen,
    message: isOpen
      ? 'Markets are open — trading is live.'
      : 'Markets are closed right now. Regular trading hours are 9:30 AM–4:00 PM ET, Monday through Friday — prices shown reflect the last close.',
  }
}
