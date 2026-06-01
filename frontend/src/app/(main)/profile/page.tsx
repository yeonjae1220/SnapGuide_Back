'use client'

export const dynamic = 'force-dynamic'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { api } from '@/lib/api'
import { useAuthStore } from '@/stores/useAuthStore'
import { useI18n } from '@/i18n/I18nProvider'
import { SUPPORTED_UI_LANGUAGES } from '@/i18n/messages'
import { useTheme, type AppTheme } from '@/theme/ThemeProvider'

const THEME_OPTIONS: Array<{ value: AppTheme; labelKey: 'settings.themeLight' | 'settings.themeDark'; icon: string }> = [
  { value: 'light', labelKey: 'settings.themeLight', icon: '☀️' },
  { value: 'dark', labelKey: 'settings.themeDark', icon: '🌙' },
]

export default function ProfilePage() {
  const { t, language, setLanguage } = useI18n()
  const { theme, setTheme } = useTheme()
  const router = useRouter()
  const { accessToken, email, clearTokens } = useAuthStore()

  useEffect(() => {
    if (accessToken === null) router.replace('/')
  }, [accessToken, router])

  const handleLogout = async () => {
    try {
      await api.post('/api/auth/logout', { accessToken })
    } finally {
      clearTokens()
      router.replace('/')
    }
  }

  const handleDelete = async () => {
    if (!confirm(t('profile.deleteConfirm'))) return
    try {
      await api.post('/api/auth/delete', { accessToken })
      clearTokens()
      router.replace('/')
    } catch {
      alert(t('common.error'))
    }
  }

  if (!accessToken) return null

  return (
    <div className="mx-auto max-w-sm p-6">
      <div className="mb-6 flex items-center gap-4">
        <div className="flex h-16 w-16 items-center justify-center rounded-full bg-gradient-to-br from-orange-400 to-pink-600 text-2xl text-white">
          👤
        </div>
        <div>
          <p className="text-xs text-subtle">{t('profile.email')}</p>
          <p className="text-sm font-medium text-ink">
            {email || t('profile.noEmail')}
          </p>
        </div>
      </div>

      <div className="space-y-3">
        {/* appearance */}
        <div className="flex items-center justify-between rounded-2xl border border-line bg-surface p-4 shadow-card transition-colors duration-200">
          <p className="text-sm font-medium text-ink">{t('settings.appearance')}</p>
          <div className="grid grid-cols-2 rounded-xl bg-surface-muted p-1">
            {THEME_OPTIONS.map((option) => {
              const active = theme === option.value
              return (
                <button
                  key={option.value}
                  type="button"
                  aria-pressed={active}
                  onClick={() => setTheme(option.value)}
                  className={`flex min-w-20 items-center justify-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-semibold transition-all focus:outline-none focus:ring-2 focus:ring-accent/30 ${
                    active
                      ? 'bg-surface-elevated text-ink shadow-sm'
                      : 'text-muted hover:text-ink'
                  }`}
                >
                  <span aria-hidden>{option.icon}</span>
                  {t(option.labelKey)}
                </button>
              )
            })}
          </div>
        </div>

        {/* language */}
        <div className="flex items-center justify-between rounded-2xl border border-line bg-surface p-4 shadow-card transition-colors duration-200">
          <div>
            <p className="text-sm font-medium text-ink">{t('settings.uiLanguage')}</p>
          </div>
          <select
            value={language}
            onChange={(e) => setLanguage(e.target.value)}
            className="rounded-lg border border-line bg-field px-2 py-1 text-sm text-ink outline-none transition-colors focus:border-accent/60 focus:ring-2 focus:ring-accent/15"
          >
            {SUPPORTED_UI_LANGUAGES.map((l) => (
              <option key={l} value={l}>
                {l.toUpperCase()}
              </option>
            ))}
          </select>
        </div>

        <button
          onClick={handleLogout}
          className="w-full rounded-2xl border border-line bg-surface p-4 text-left text-sm font-medium text-ink shadow-card transition hover:bg-surface-elevated"
        >
          {t('nav.logout')}
        </button>

        <button
          onClick={handleDelete}
          className="w-full rounded-2xl border border-danger/20 bg-surface p-4 text-left text-sm font-medium text-danger shadow-card transition hover:bg-danger-soft"
        >
          {t('profile.deleteAccount')}
        </button>
      </div>
    </div>
  )
}
