'use client'

import { Navbar } from '@/components/Navbar'
import { InitAuth } from '@/components/InitAuth'

export default function MainLayout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <InitAuth />
      <div className="flex min-h-screen flex-col bg-app pb-16 text-ink transition-colors duration-200">
        <main className="flex-1">{children}</main>
        <Navbar />
      </div>
    </>
  )
}
