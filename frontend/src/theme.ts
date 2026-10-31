export type Theme = 'light' | 'dark'

const THEME_KEY = 'ticketwise.theme'

export function savedTheme(): Theme | null {
  try {
    const value = localStorage.getItem(THEME_KEY)
    return value === 'light' || value === 'dark' ? value : null
  } catch {
    return null
  }
}

export function activeTheme(): Theme {
  return savedTheme() ?? (window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')
}

export function applySavedTheme() {
  const theme = savedTheme()
  if (theme) {
    document.documentElement.dataset.theme = theme
  }
}

export function chooseTheme(theme: Theme) {
  document.documentElement.dataset.theme = theme
  try {
    localStorage.setItem(THEME_KEY, theme)
  } catch {
    // The choice still applies until the page is closed.
  }
}
