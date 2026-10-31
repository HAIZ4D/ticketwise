import { act, renderHook } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useNow } from './useNow.ts'

describe('useNow', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('moves forward every 30 seconds', () => {
    vi.useFakeTimers({ now: 1_000 })
    const { result } = renderHook(() => useNow())
    expect(result.current).toBe(1_000)

    act(() => {
      vi.advanceTimersByTime(30_000)
    })

    expect(result.current).toBe(31_000)
  })

  it('stops the timer when the page closes', () => {
    vi.useFakeTimers()
    const { unmount } = renderHook(() => useNow())

    unmount()

    expect(vi.getTimerCount()).toBe(0)
  })
})
