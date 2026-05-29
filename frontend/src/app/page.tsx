'use client'

export const dynamic = 'force-dynamic'


import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { useAuthStore } from '@/stores/useAuthStore'
import { LandingHero } from '@/components/landing/LandingHero'
import { AuthPanel } from '@/components/landing/AuthPanel'
import { InitAuth } from '@/components/InitAuth'

export default function LandingPage() {
  const accessToken = useAuthStore((s) => s.accessToken)
  const router = useRouter()

  useEffect(() => {
    if (accessToken) router.replace('/feed')
  }, [accessToken, router])

  return (
    <>
      <InitAuth />
      <div className="flex min-h-screen bg-black">
        <LandingHero />
        <AuthPanel />
      </div>
    </>
  )
}
