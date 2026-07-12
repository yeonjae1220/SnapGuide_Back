'use client'

export const dynamic = 'force-dynamic'

import { Suspense, useEffect } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { useAuthStore } from '@/stores/useAuthStore'
import { LandingHero } from '@/components/landing/LandingHero'
import { AuthPanel } from '@/components/landing/AuthPanel'
import { InitAuth } from '@/components/InitAuth'
import { useI18n } from '@/i18n/I18nProvider'

function LandingContent() {
  const { t } = useI18n()
  const accessToken = useAuthStore((s) => s.accessToken)
  const initialized = useAuthStore((s) => s.initialized)
  const router = useRouter()
  const searchParams = useSearchParams()
  const hasCode = !!searchParams.get('code')

  useEffect(() => {
    if (accessToken) router.replace('/feed')
  }, [accessToken, router])

  // OAuth 코드 교환 중에는 랜딩 UI 대신 로딩 화면 표시
  if ((hasCode && !accessToken) || (!initialized && !accessToken)) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-app text-ink transition-colors duration-200">
        <div className="flex flex-col items-center gap-4">
          <div className="h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent" />
          <p className="text-sm text-muted">Signing in...</p>
        </div>
      </div>
    )
  }

  return (
    <>
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-accent focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:text-white"
      >
        {t('landing.skipToContent')}
      </a>
      <main id="main-content" className="flex min-h-screen bg-app text-ink transition-colors duration-200">
        <LandingHero />
        <AuthPanel />
      </main>
    </>
  )
}

export default function LandingPage() {
  return (
    <>
      <InitAuth />
      <Suspense fallback={<div className="min-h-screen bg-app" />}>
        <LandingContent />
      </Suspense>
    </>
  )
}
