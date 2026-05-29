import type { Metadata, Viewport } from 'next'
import { DM_Sans, Playfair_Display } from 'next/font/google'
import './globals.css'
import { I18nProvider } from '@/i18n/I18nProvider'
import { QueryProvider } from '@/components/QueryProvider'

const dmSans = DM_Sans({
  subsets: ['latin'],
  variable: '--font-sans',
  weight: ['300', '400', '500', '600', '700'],
})

const playfair = Playfair_Display({
  subsets: ['latin'],
  variable: '--font-display',
  weight: ['700', '800'],
})

export const metadata: Metadata = {
  title: 'SnapGuide',
  description: '여행의 순간을 가이드로, 세상과 나누다',
  manifest: '/manifest.json',
}

export const viewport: Viewport = {
  themeColor: '#bc1888',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body className={`${dmSans.variable} ${playfair.variable}`}>
        <QueryProvider>
          <I18nProvider>{children}</I18nProvider>
        </QueryProvider>
      </body>
    </html>
  )
}
