'use client'

import { createContext, useContext, useEffect, useMemo, useRef, useState } from 'react'

export type ThemePreference = 'system' | 'light' | 'dark'
// 하위호환: 기존 소비처(profile)가 2-way 타입을 import한다.
export type AppTheme = 'light' | 'dark'

type ThemeContextValue = {
  preference: ThemePreference
  resolvedTheme: 'light' | 'dark'
  setPreference: (preference: ThemePreference) => void
  // 하위호환: 기존 소비처(profile/feed)가 theme/setTheme(2-way)를 사용한다.
  theme: 'light' | 'dark'
  setTheme: (theme: 'light' | 'dark') => void
}

const STORAGE_KEY = 'snapguide.theme'
const THEME_QUERY = '(prefers-color-scheme: dark)'

const ThemeContext = createContext<ThemeContextValue | null>(null)

// 저장된 선호가 없거나 무효하면 dark 기본. 'system'은 사용자가 명시적으로 고를 때만.
function normalizeThemePreference(value: string | null): ThemePreference {
  return value === 'light' || value === 'dark' || value === 'system' ? value : 'dark'
}

function getSystemTheme(): 'light' | 'dark' {
  if (typeof window === 'undefined') return 'dark'
  return window.matchMedia(THEME_QUERY).matches ? 'dark' : 'light'
}

function applyTheme(
  preference: ThemePreference,
  systemTheme: 'light' | 'dark' = getSystemTheme(),
): 'light' | 'dark' {
  const resolvedTheme = preference === 'system' ? systemTheme : preference
  const root = document.documentElement
  root.dataset.theme = resolvedTheme
  root.style.colorScheme = resolvedTheme
  return resolvedTheme
}

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [preference, setPreferenceState] = useState<ThemePreference>('dark')
  const [resolvedTheme, setResolvedTheme] = useState<'light' | 'dark'>('dark')
  const preferenceRef = useRef<ThemePreference>('dark')

  useEffect(() => {
    const storedPreference = normalizeThemePreference(window.localStorage.getItem(STORAGE_KEY))
    const media = window.matchMedia(THEME_QUERY)

    preferenceRef.current = storedPreference
    setPreferenceState(storedPreference)
    setResolvedTheme(applyTheme(storedPreference, media.matches ? 'dark' : 'light'))

    const handleSystemThemeChange = (event: MediaQueryListEvent) => {
      setResolvedTheme(applyTheme(preferenceRef.current, event.matches ? 'dark' : 'light'))
    }
    media.addEventListener('change', handleSystemThemeChange)
    return () => media.removeEventListener('change', handleSystemThemeChange)
  }, [])

  const setPreference = (next: ThemePreference) => {
    preferenceRef.current = next
    setPreferenceState(next)
    window.localStorage.setItem(STORAGE_KEY, next)
    setResolvedTheme(applyTheme(next))
  }

  const value = useMemo<ThemeContextValue>(
    () => ({
      preference,
      resolvedTheme,
      setPreference,
      theme: resolvedTheme,
      setTheme: (t) => setPreference(t),
    }),
    [preference, resolvedTheme],
  )

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

export function useTheme() {
  const context = useContext(ThemeContext)
  if (!context) {
    throw new Error('useTheme must be used within ThemeProvider')
  }
  return context
}
