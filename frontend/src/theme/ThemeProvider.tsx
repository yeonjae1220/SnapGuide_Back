'use client'

import { createContext, useContext, useEffect, useMemo, useState } from 'react'

export type AppTheme = 'light' | 'dark'

type ThemeContextValue = {
  theme: AppTheme
  setTheme: (theme: AppTheme) => void
}

const THEME_STORAGE_KEY = 'snapguide.theme'
const DEFAULT_THEME: AppTheme = 'light'

const ThemeContext = createContext<ThemeContextValue | null>(null)

function isAppTheme(value: string | null): value is AppTheme {
  return value === 'light' || value === 'dark'
}

function applyTheme(theme: AppTheme) {
  document.documentElement.dataset.theme = theme
  document.documentElement.style.colorScheme = theme
}

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [theme, setThemeState] = useState<AppTheme>(() => {
    if (typeof document === 'undefined') return DEFAULT_THEME
    const currentTheme = document.documentElement.dataset.theme ?? null
    return isAppTheme(currentTheme) ? currentTheme : DEFAULT_THEME
  })

  useEffect(() => {
    const stored = window.localStorage.getItem(THEME_STORAGE_KEY)
    const nextTheme = isAppTheme(stored) ? stored : DEFAULT_THEME
    setThemeState(nextTheme)
    applyTheme(nextTheme)
  }, [])

  const setTheme = (nextTheme: AppTheme) => {
    setThemeState(nextTheme)
    window.localStorage.setItem(THEME_STORAGE_KEY, nextTheme)
    applyTheme(nextTheme)
  }

  const value = useMemo(() => ({ theme, setTheme }), [theme])

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

export function useTheme() {
  const context = useContext(ThemeContext)
  if (!context) {
    throw new Error('useTheme must be used within ThemeProvider')
  }
  return context
}
