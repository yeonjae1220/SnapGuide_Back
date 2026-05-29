'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { useAuthStore } from '@/stores/useAuthStore'
import { Navbar } from '@/components/Navbar'
import { InitAuth } from '@/components/InitAuth'

export default function MainLayout({ children }: { children: React.ReactNode }) {
  const accessToken = useAuthStore((s) => s.accessToken)
  const router = useRouter()

  useEffect(() => {
    if (accessToken === null) {
    }
  }, [accessToken, router])

  return (
    <>
      <InitAuth />
      <div className="flex min-h-screen flex-col pb-16">
        <main className="flex-1">{children}</main>
        <Navbar />
      </div>
    </>
  )
}
