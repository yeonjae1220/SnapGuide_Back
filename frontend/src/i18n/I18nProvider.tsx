'use client'

import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { MessageKeyType, messages, normalizeUiLanguage, UiLanguage } from './messages'

type I18nContextValue = {
  language: UiLanguage
  setLanguage: (language: string) => void
  t: (key: MessageKeyType) => string
}

const I18nContext = createContext<I18nContextValue | null>(null)

export function I18nProvider({ children }: { children: React.ReactNode }) {
  const [language, setLanguageState] = useState<UiLanguage>('ko')

  useEffect(() => {
    const stored =
      typeof window === 'undefined'
        ? null
        : window.localStorage.getItem('snapguide_ui_language')
    setLanguageState(
      normalizeUiLanguage(stored ?? navigator.language.slice(0, 2)),
    )
  }, [])

  const value = useMemo<I18nContextValue>(
    () => ({
      language,
      setLanguage: (next) => {
        const normalized = normalizeUiLanguage(next)
        setLanguageState(normalized)
        window.localStorage.setItem('snapguide_ui_language', normalized)
      },
      t: (key) => messages[language][key] ?? messages.ko[key] ?? key,
    }),
    [language],
  )

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
}

export function useI18n() {
  const value = useContext(I18nContext)
  if (!value) throw new Error('useI18n must be used within I18nProvider')
  return value
}
