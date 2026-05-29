'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { api } from '@/lib/api'
import { useAuthStore } from '@/stores/useAuthStore'
import { useI18n } from '@/i18n/I18nProvider'
import { SUPPORTED_UI_LANGUAGES } from '@/i18n/messages'

export function AuthPanel() {
  const { t, language, setLanguage } = useI18n()
  const router = useRouter()
  const setTokens = useAuthStore((s) => s.setTokens)

  const [tab, setTab] = useState<'login' | 'signup'>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPwdHints, setShowPwdHints] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const pwdChecks = {
    len: password.length >= 8,
    upper: /[A-Z]/.test(password),
    lower: /[a-z]/.test(password),
    digit: /[0-9]/.test(password),
  }

  const handleLogin = async () => {
    setError('')
    setLoading(true)
    try {
      const { data } = await api.post('/api/auth/login', { email, password })
      setTokens(data.accessToken, email)
      router.replace('/feed')
    } catch {
      setError(t('common.error'))
    } finally {
      setLoading(false)
    }
  }

  const handleSignup = async () => {
    setError('')
    setLoading(true)
    try {
      await api.post('/api/auth/signup', { email, password })
      await handleLogin()
    } catch {
      setError(t('common.error'))
      setLoading(false)
    }
  }

  const handleGoogle = () => {
    window.location.href = '/oauth2/authorization/google'
  }

  return (
    <div className="flex w-full flex-col justify-center bg-white px-8 py-12 lg:w-[480px]">
      <div className="mx-auto w-full max-w-sm">
        <div className="mb-2 text-2xl font-extrabold ig-text lg:hidden">SnapGuide</div>
        <p className="mb-8 text-sm text-gray-500">{t('auth.or')} {t('auth.login').toLowerCase()} / {t('auth.signup').toLowerCase()}</p>

        {/* tabs */}
        <div className="mb-6 flex gap-0 rounded-xl bg-gray-100 p-1">
          {(['login', 'signup'] as const).map((v) => (
            <button
              key={v}
              onClick={() => setTab(v)}
              className={`flex-1 rounded-lg py-2 text-sm font-medium transition-all ${
                tab === v ? 'bg-white shadow text-gray-900' : 'text-gray-500'
              }`}
            >
              {v === 'login' ? t('auth.login') : t('auth.signup')}
            </button>
          ))}
        </div>

        <div className="space-y-3">
          <input
            type="email"
            placeholder={t('auth.email')}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm outline-none focus:border-pink-400 focus:ring-2 focus:ring-pink-100"
          />
          <input
            type="password"
            placeholder={t('auth.password')}
            value={password}
            onChange={(e) => {
              setPassword(e.target.value)
              if (tab === 'signup') setShowPwdHints(true)
            }}
            onFocus={() => tab === 'signup' && setShowPwdHints(true)}
            className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm outline-none focus:border-pink-400 focus:ring-2 focus:ring-pink-100"
          />

          {tab === 'signup' && showPwdHints && (
            <ul className="rounded-xl bg-gray-50 px-4 py-3 text-xs space-y-1">
              {(
                [
                  [pwdChecks.len, 'auth.pwdLen'],
                  [pwdChecks.upper, 'auth.pwdUpper'],
                  [pwdChecks.lower, 'auth.pwdLower'],
                  [pwdChecks.digit, 'auth.pwdDigit'],
                ] as const
              ).map(([ok, key]) => (
                <li key={key} className={ok ? 'text-green-600' : 'text-gray-400'}>
                  {ok ? '✓' : '○'} {t(key)}
                </li>
              ))}
            </ul>
          )}

          {error && <p className="text-xs text-red-500">{error}</p>}

          <button
            onClick={tab === 'login' ? handleLogin : handleSignup}
            disabled={loading}
            className="ig-bg w-full rounded-xl py-3 text-sm font-semibold text-white transition-opacity hover:opacity-90 disabled:opacity-60"
          >
            {loading ? t('common.loading') : tab === 'login' ? t('auth.login') : t('auth.signup')}
          </button>

          <div className="flex items-center gap-3 text-xs text-gray-400">
            <div className="h-px flex-1 bg-gray-200" />
            {t('auth.or')}
            <div className="h-px flex-1 bg-gray-200" />
          </div>

          <button
            onClick={handleGoogle}
            className="flex w-full items-center justify-center gap-3 rounded-xl border border-gray-200 py-3 text-sm font-medium text-gray-700 transition hover:bg-gray-50"
          >
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
              <path d="M17.64 9.205c0-.639-.057-1.252-.164-1.841H9v3.481h4.844a4.14 4.14 0 0 1-1.796 2.716v2.259h2.908c1.702-1.567 2.684-3.875 2.684-6.615Z" fill="#4285F4"/>
              <path d="M9 18c2.43 0 4.467-.806 5.956-2.18l-2.908-2.259c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 0 0 9 18Z" fill="#34A853"/>
              <path d="M3.964 10.71A5.41 5.41 0 0 1 3.682 9c0-.593.102-1.17.282-1.71V4.958H.957A8.996 8.996 0 0 0 0 9c0 1.452.348 2.827.957 4.042l3.007-2.332Z" fill="#FBBC05"/>
              <path d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 0 0 .957 4.958L3.964 7.29C4.672 5.163 6.656 3.58 9 3.58Z" fill="#EA4335"/>
            </svg>
            {t('auth.loginWithGoogle')}
          </button>
        </div>

        {/* language selector */}
        <div className="mt-8 flex items-center justify-end gap-2">
          <span className="text-xs text-gray-400">{t('settings.uiLanguage')}</span>
          <select
            value={language}
            onChange={(e) => setLanguage(e.target.value)}
            className="rounded-lg border border-gray-200 px-2 py-1 text-xs text-gray-600 outline-none"
          >
            {SUPPORTED_UI_LANGUAGES.map((l) => (
              <option key={l} value={l}>
                {l.toUpperCase()}
              </option>
            ))}
          </select>
        </div>
      </div>
    </div>
  )
}
