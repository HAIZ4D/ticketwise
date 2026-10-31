import { describe, expect, it } from 'vitest'
import { initials, timeLeft } from './format.ts'

const NOW = Date.parse('2026-11-01T12:00:00Z')

function dueIn(minutes: number): string {
  return new Date(NOW + minutes * 60_000).toISOString()
}

describe('timeLeft', () => {
  it.each([
    [45, '45m left'],
    [60, '1h 0m left'],
    [134, '2h 14m left'],
    [1440, '1d 0h left'],
    [2940, '2d 1h left'],
    [0, 'under 1m left'],
    [-1, '1m late'],
    [-195, '3h 15m late'],
    [-3000, '2d 2h late'],
  ])('shows %i minutes as "%s"', (minutes, text) => {
    expect(timeLeft(dueIn(minutes), NOW)).toBe(text)
  })
})

describe('initials', () => {
  it.each([
    ['aina@example.com', 'AI'],
    ['aina.rahman@example.com', 'AR'],
    ['first-last@example.com', 'FL'],
    ['b@example.com', 'B'],
  ])('turns %s into %s', (email, text) => {
    expect(initials(email)).toBe(text)
  })
})
