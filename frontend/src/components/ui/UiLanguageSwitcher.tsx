'use client'

import { useI18n } from '@/i18n/I18nProvider'
import { UI_LANGUAGE_OPTIONS, type UiLanguage } from '@/i18n/messages'

function displayName(code: UiLanguage, uiLanguage: UiLanguage): string {
  try {
    return new Intl.DisplayNames([uiLanguage], { type: 'language' }).of(code) ?? code
  } catch {
    return code
  }
}

/**
 * 비로그인 사용자도 UI 언어를 바꿀 수 있는 경량 스위처.
 * 네이티브 <select>라 키보드/스크린리더 접근성이 기본 보장된다.
 */
export function UiLanguageSwitcher({ className = '' }: { className?: string }) {
  const { language, setLanguage, t } = useI18n()

  return (
    <label className={`relative inline-flex items-center ${className}`}>
      <span className="sr-only">{t('settings.uiLanguage')}</span>
      <span aria-hidden className="pointer-events-none absolute left-2.5 text-sm">🌐</span>
      <select
        value={language}
        onChange={(event) => setLanguage(event.target.value)}
        className="appearance-none rounded-lg border border-line bg-field pl-8 pr-7 py-1.5 text-xs text-ink outline-none transition-colors hover:border-accent/60 focus:border-accent/60 focus:ring-2 focus:ring-accent/15"
      >
        {UI_LANGUAGE_OPTIONS.map((opt) => (
          <option key={opt.code} value={opt.code}>
            {opt.flag} {displayName(opt.code, language)}
          </option>
        ))}
      </select>
      <span aria-hidden className="pointer-events-none absolute right-2.5 text-xs text-subtle">▾</span>
    </label>
  )
}
