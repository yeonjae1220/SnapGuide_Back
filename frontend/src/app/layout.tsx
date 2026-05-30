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

const BASE_URL = 'https://snapguide.mungji.com'

export const metadata: Metadata = {
  metadataBase: new URL(BASE_URL),
  title: {
    default: 'SnapGuide',
    template: '%s | SnapGuide',
  },
  description: '여행의 순간을 가이드로, 세상과 나누다. 나만의 여행 가이드를 만들고 전 세계와 공유하세요.',
  keywords: ['여행 가이드', '여행', '관광', 'travel guide', 'travel', 'snapguide'],
  authors: [{ name: 'SnapGuide' }],
  creator: 'SnapGuide',
  manifest: '/manifest.json',
  openGraph: {
    type: 'website',
    locale: 'ko_KR',
    url: BASE_URL,
    siteName: 'SnapGuide',
    title: 'SnapGuide — 여행의 순간을 가이드로',
    description: '여행의 순간을 가이드로, 세상과 나누다. 나만의 여행 가이드를 만들고 전 세계와 공유하세요.',
    images: [
      {
        url: '/icons/og-image.png',
        width: 1200,
        height: 630,
        alt: 'SnapGuide',
      },
    ],
  },
  twitter: {
    card: 'summary_large_image',
    title: 'SnapGuide — 여행의 순간을 가이드로',
    description: '여행의 순간을 가이드로, 세상과 나누다.',
    images: ['/icons/og-image.png'],
  },
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
    },
  },
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
