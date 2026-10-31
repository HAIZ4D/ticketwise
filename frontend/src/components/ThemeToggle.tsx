import { Moon, Sun } from 'lucide-react'
import { useState } from 'react'
import { activeTheme, chooseTheme } from '../theme.ts'

export function ThemeToggle() {
  const [theme, setTheme] = useState(activeTheme)
  const next = theme === 'dark' ? 'light' : 'dark'

  function toggle() {
    chooseTheme(next)
    setTheme(next)
  }

  return (
    <button type="button" className="icon-button" aria-label={`Switch to ${next} mode`} onClick={toggle}>
      {theme === 'dark' ? <Sun size={15} aria-hidden="true" /> : <Moon size={15} aria-hidden="true" />}
    </button>
  )
}
