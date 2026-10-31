const dateFormat = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' })

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
