'use client'

import { Navbar } from '@/components/Navbar'
import { InitAuth } from '@/components/InitAuth'

export default function MainLayout({ children }: { children: React.ReactNode }) {
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
