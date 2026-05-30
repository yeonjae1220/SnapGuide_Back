'use client'

export const dynamic = 'force-dynamic'

import { Suspense, useEffect } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { useAuthStore } from '@/stores/useAuthStore'
import { LandingHero } from '@/components/landing/LandingHero'
import { AuthPanel } from '@/components/landing/AuthPanel'
import { InitAuth } from '@/components/InitAuth'

function LandingContent() {
  const accessToken = useAuthStore((s) => s.accessToken)
  const router = useRouter()
  const searchParams = useSearchParams()
  const hasCode = !!searchParams.get('code')

  useEffect(() => {
    if (accessToken) router.replace('/feed')
  }, [accessToken, router])

  // OAuth 코드 교환 중에는 랜딩 UI 대신 로딩 화면 표시
  if (hasCode && !accessToken) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-black">
        <div className="flex flex-col items-center gap-4 text-white">
          <div className="h-8 w-8 animate-spin rounded-full border-2 border-white border-t-transparent" />
          <p className="text-sm text-gray-400">Signing in…</p>
        </div>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen bg-black">
      <LandingHero />
      <AuthPanel />
    </div>
  )
}

export default function LandingPage() {
  return (
    <>
      <InitAuth />
      <Suspense fallback={<div className="min-h-screen bg-black" />}>
        <LandingContent />
      </Suspense>
    </>
  )
}
