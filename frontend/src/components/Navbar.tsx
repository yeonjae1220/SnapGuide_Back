'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useI18n } from '@/i18n/I18nProvider'
import { useAuthStore } from '@/stores/useAuthStore'
import { api } from '@/lib/api'
import { useRouter } from 'next/navigation'

const NAV_ITEMS = [
  { href: '/feed', labelKey: 'nav.explore' as const, icon: '🗺️' },
  { href: '/guides', labelKey: 'nav.myGuides' as const, icon: '📖' },
  { href: '/upload', labelKey: 'nav.upload' as const, icon: '➕' },
  { href: '/profile', labelKey: 'nav.profile' as const, icon: '👤' },
]

export function Navbar() {
  const { t } = useI18n()
  const pathname = usePathname()
  const { accessToken, clearTokens } = useAuthStore()
  const router = useRouter()

  const handleLogout = async () => {
    try {
      await api.post('/api/auth/logout', { accessToken })
    } finally {
      clearTokens()
      router.replace('/')
    }
  }

  return (
    <nav className="fixed inset-x-0 bottom-0 z-50 border-t border-gray-200 bg-white">
      <div className="flex">
        {NAV_ITEMS.map(({ href, labelKey, icon }) => {
          const active = pathname === href || pathname.startsWith(href + '/')
          return (
            <Link
              key={href}
              href={href}
              className={`flex flex-1 flex-col items-center gap-0.5 py-2 text-[10px] font-medium transition-colors ${
                active ? 'text-pink-600' : 'text-gray-400'
              }`}
            >
              <span className="text-lg leading-none">{icon}</span>
              {t(labelKey)}
            </Link>
          )
        })}
        {accessToken && (
          <button
            onClick={handleLogout}
            className="flex flex-1 flex-col items-center gap-0.5 py-2 text-[10px] font-medium text-gray-400 transition-colors hover:text-red-500"
          >
            <span className="text-lg leading-none">🚪</span>
            {t('nav.logout')}
          </button>
        )}
      </div>
    </nav>
  )
}
