import type { Metadata, Viewport } from 'next'
import { DM_Sans, Playfair_Display } from 'next/font/google'
import { cookies, headers } from 'next/headers'
import './globals.css'
import { normalizeUiLanguage, UI_LANGUAGE_KEY } from '@/i18n/messages'
import { I18nProvider } from '@/i18n/I18nProvider'
import { QueryProvider } from '@/components/QueryProvider'
import { ThemeProvider } from '@/theme/ThemeProvider'
import { RegisterServiceWorker } from '@/components/RegisterServiceWorker'

const dmSans = DM_Sans({
  subsets: ['latin'],
  variable: '--font-sans',
  weight: ['300', '400', '500', '600', '700'],
})

const playfair = Playfair_Display({
  subsets: ['latin'],
  variable: '--font-display',
  weight: ['700', '800'],
  preload: false,
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
  manifest: '/manifest.webmanifest',
  openGraph: {
    type: 'website',
    locale: 'ko_KR',
    url: BASE_URL,
    siteName: 'SnapGuide',
    title: 'SnapGuide — 여행의 순간을 가이드로',
    description: '여행의 순간을 가이드로, 세상과 나누다. 나만의 여행 가이드를 만들고 전 세계와 공유하세요.',
    images: [
      {
        url: '/images/snapguide-hero.webp',
        width: 1672,
        height: 941,
        alt: 'SnapGuide',
      },
    ],
  },
  twitter: {
    card: 'summary_large_image',
    title: 'SnapGuide — 여행의 순간을 가이드로',
    description: '여행의 순간을 가이드로, 세상과 나누다.',
    images: ['/images/snapguide-hero.webp'],
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

// 페인트 전에 테마를 적용해 플래시 방지. 기본 dark, 'system'은 prefers-color-scheme로 해석.
const themeInitScript = `(() => {
  try {
    const pref = localStorage.getItem('snapguide.theme') || 'dark';
    const sysDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    const resolved = pref === 'dark' || (pref === 'system' && sysDark) ? 'dark' : 'light';
    const r = document.documentElement;
    r.dataset.theme = resolved;
    r.style.colorScheme = resolved;
  } catch (_) {
    const r = document.documentElement;
    r.dataset.theme = 'dark';
    r.style.colorScheme = 'dark';
  }
})();`

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  const [headerList, cookieStore] = await Promise.all([headers(), cookies()])
  const nonce = headerList.get('x-nonce') ?? ''
  const initialLanguage = normalizeUiLanguage(cookieStore.get(UI_LANGUAGE_KEY)?.value)
  return (
    <html lang={initialLanguage} data-theme="dark" suppressHydrationWarning>
      <head>
        <script nonce={nonce} dangerouslySetInnerHTML={{ __html: themeInitScript }} />
      </head>
      <body className={`${dmSans.variable} ${playfair.variable}`} data-nonce={nonce}>
        <ThemeProvider>
          <QueryProvider>
            <I18nProvider initialLanguage={initialLanguage}>{children}</I18nProvider>
            <RegisterServiceWorker />
          </QueryProvider>
        </ThemeProvider>
      </body>
    </html>
  )
}
