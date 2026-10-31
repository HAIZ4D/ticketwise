import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { applySavedTheme } from '../theme.ts'
import { ThemeToggle } from './ThemeToggle.tsx'

function systemPrefersDark(prefersDark: boolean) {
  vi.stubGlobal('matchMedia', () => ({ matches: prefersDark }))
}

describe('ThemeToggle', () => {
  afterEach(() => {
    delete document.documentElement.dataset.theme
  })

  it('follows the system until the user chooses', () => {
    systemPrefersDark(true)

    render(<ThemeToggle />)

    expect(screen.getByRole('button', { name: 'Switch to light mode' })).toBeInTheDocument()
    expect(document.documentElement.dataset.theme).toBeUndefined()
  })

  it('applies the choice and offers the opposite', async () => {
    systemPrefersDark(false)
    render(<ThemeToggle />)

    await userEvent.setup().click(screen.getByRole('button', { name: 'Switch to dark mode' }))

    expect(document.documentElement.dataset.theme).toBe('dark')
    expect(screen.getByRole('button', { name: 'Switch to light mode' })).toBeInTheDocument()
  })

  it('remembers the choice after a reload', async () => {
    systemPrefersDark(false)
    render(<ThemeToggle />)
    await userEvent.setup().click(screen.getByRole('button', { name: 'Switch to dark mode' }))
    delete document.documentElement.dataset.theme

    applySavedTheme()

    expect(document.documentElement.dataset.theme).toBe('dark')
  })

  it('a saved choice beats the system setting', async () => {
    systemPrefersDark(false)
    const { unmount } = render(<ThemeToggle />)
    await userEvent.setup().click(screen.getByRole('button', { name: 'Switch to dark mode' }))
    unmount()

    systemPrefersDark(false)
    render(<ThemeToggle />)

    expect(screen.getByRole('button', { name: 'Switch to light mode' })).toBeInTheDocument()
  })

  it('ignores a saved value it does not know', () => {
    systemPrefersDark(true)
    localStorage.setItem('ticketwise.theme', 'purple')

    applySavedTheme()
    render(<ThemeToggle />)

    expect(document.documentElement.dataset.theme).toBeUndefined()
    expect(screen.getByRole('button', { name: 'Switch to light mode' })).toBeInTheDocument()
  })
})
