'use client'

export const dynamic = 'force-dynamic'


import { useRouter } from 'next/navigation'
import { api } from '@/lib/api'
import { useAuthStore } from '@/stores/useAuthStore'
import { useI18n } from '@/i18n/I18nProvider'
import { SUPPORTED_UI_LANGUAGES } from '@/i18n/messages'

export default function ProfilePage() {
  const { t, language, setLanguage } = useI18n()
  const router = useRouter()
  const { accessToken, email, clearTokens } = useAuthStore()

  if (!accessToken) {
    return (
      <div className="flex h-[60vh] flex-col items-center justify-center gap-4">
        <p className="text-gray-500">{t('nav.login')}</p>
        <button
          onClick={() => router.replace('/')}
          className="ig-bg rounded-xl px-6 py-2 text-sm font-semibold text-white"
        >
          {t('nav.login')}
        </button>
      </div>
    )
  }

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

  return (
    <div className="mx-auto max-w-sm p-6">
      <div className="mb-6 flex items-center gap-4">
        <div className="flex h-16 w-16 items-center justify-center rounded-full bg-gradient-to-br from-orange-400 to-pink-600 text-2xl text-white">
          👤
        </div>
        <div>
          <p className="text-xs text-gray-400">{t('profile.email')}</p>
          <p className="text-sm font-medium text-gray-800">
            {email || t('profile.noEmail')}
          </p>
        </div>
      </div>

      <div className="space-y-3">
        {/* language */}
        <div className="flex items-center justify-between rounded-2xl border border-gray-100 bg-white p-4">
          <div>
            <p className="text-sm font-medium">{t('settings.uiLanguage')}</p>
          </div>
          <select
            value={language}
            onChange={(e) => setLanguage(e.target.value)}
            className="rounded-lg border border-gray-200 px-2 py-1 text-sm outline-none"
          >
            {SUPPORTED_UI_LANGUAGES.map((l) => (
              <option key={l} value={l}>
                {l.toUpperCase()}
              </option>
            ))}
          </select>
        </div>

        {/* logout */}
        <button
          onClick={handleLogout}
          className="w-full rounded-2xl border border-gray-100 bg-white p-4 text-left text-sm font-medium text-gray-700 hover:bg-gray-50 transition"
        >
          {t('nav.logout')}
        </button>

        {/* delete */}
        <button
          onClick={handleDelete}
          className="w-full rounded-2xl border border-red-100 bg-white p-4 text-left text-sm font-medium text-red-500 hover:bg-red-50 transition"
        >
          {t('profile.deleteAccount')}
        </button>
      </div>
    </div>
  )
}
