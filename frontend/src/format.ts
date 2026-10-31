const dateFormat = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' })

const MINUTE = 60_000
const MINUTES_PER_HOUR = 60
const MINUTES_PER_DAY = 1440

export function label(value: string): string {
  const text = value.replaceAll('_', ' ').toLowerCase()
  return text.charAt(0).toUpperCase() + text.slice(1)
}

export function count(total: number, noun: string): string {
  return `${total} ${noun}${total === 1 ? '' : 's'}`
}

export function formatDate(isoDate: string): string {
  return dateFormat.format(new Date(isoDate))
}

export function timeLeft(dueAt: string, now: number): string {
  const minutes = Math.round((new Date(dueAt).getTime() - now) / MINUTE)
  return `${duration(Math.abs(minutes))} ${minutes >= 0 ? 'left' : 'late'}`
}

export function initials(email: string): string {
  const [first, second] = email.split('@')[0].split(/[._-]/)
  return ((first?.[0] ?? '') + (second?.[0] ?? first?.[1] ?? '')).toUpperCase()
}

function duration(minutes: number): string {
  if (minutes < 1) {
    return 'under 1m'
  }
  const days = Math.floor(minutes / MINUTES_PER_DAY)
  const hours = Math.floor(minutes / MINUTES_PER_HOUR) % 24
  if (days > 0) {
    return `${days}d ${hours}h`
  }
  return hours > 0 ? `${hours}h ${minutes % MINUTES_PER_HOUR}m` : `${minutes}m`
}
