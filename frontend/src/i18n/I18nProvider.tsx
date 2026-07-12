'use client'

import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import {
  DEFAULT_UI_LANGUAGE,
  MessageKeyType,
  messages,
  normalizeUiLanguage,
  UI_LANGUAGE_KEY,
  UiLanguage,
} from './messages'

type I18nContextValue = {
  language: UiLanguage
  setLanguage: (language: string) => void
  t: (key: MessageKeyType) => string
}

function readLangCookie(): string | null {
  if (typeof document === 'undefined') return null
  const m = document.cookie.match(new RegExp(`(?:^|; )${UI_LANGUAGE_KEY}=([^;]*)`))
  return m ? decodeURIComponent(m[1]) : null
}

function writeLangCookie(lang: string): void {
  const secure = location.protocol === 'https:' ? '; Secure' : ''
  document.cookie = `${UI_LANGUAGE_KEY}=${lang}; path=/; max-age=31536000; SameSite=Lax${secure}`
}

const I18nContext = createContext<I18nContextValue | null>(null)

export function I18nProvider({
  children,
  initialLanguage = DEFAULT_UI_LANGUAGE,
}: {
  children: React.ReactNode
  initialLanguage?: UiLanguage
}) {
  // 서버가 쿠키로 결정한 언어를 초기값으로 사용 → SSR/hydration 첫 렌더 일치.
  const [language, setLanguageState] = useState<UiLanguage>(initialLanguage)

  // 쿠키 > localStorage > 브라우저 순으로 해석하고 결과를 쿠키에 백필해
  // 기존(쿠키 없는) 사용자도 다음 로드부터 서버 SSR <html lang>이 정확해진다.
  // (useEffect는 클라이언트에서만 실행되므로 window/document는 항상 존재)
  useEffect(() => {
    const stored = readLangCookie() ?? window.localStorage.getItem(UI_LANGUAGE_KEY)
    const resolved = normalizeUiLanguage(stored ?? navigator.language.slice(0, 2))
    setLanguageState(resolved)
    window.localStorage.setItem(UI_LANGUAGE_KEY, resolved)
    writeLangCookie(resolved)
  }, [])

  // 접근성: <html lang>을 현재 UI 언어와 동기화 (스크린리더 발음 엔진 정합)
  useEffect(() => {
    document.documentElement.lang = language
  }, [language])

  const value = useMemo<I18nContextValue>(
    () => ({
      language,
      setLanguage: (next) => {
        const normalized = normalizeUiLanguage(next)
        setLanguageState(normalized)
        window.localStorage.setItem(UI_LANGUAGE_KEY, normalized)
        writeLangCookie(normalized)
      },
      t: (key) => messages[language][key] ?? messages[DEFAULT_UI_LANGUAGE][key] ?? key,
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
