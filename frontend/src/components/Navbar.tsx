'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useI18n } from '@/i18n/I18nProvider'

const NAV_ITEMS = [
  { href: '/feed', labelKey: 'nav.explore' as const, icon: '🗺️' },
  { href: '/guides', labelKey: 'nav.myGuides' as const, icon: '📖' },
  { href: '/upload', labelKey: 'nav.upload' as const, icon: '➕' },
  { href: '/profile', labelKey: 'nav.profile' as const, icon: '👤' },
]

export function Navbar() {
  const { t } = useI18n()
  const pathname = usePathname()

  return (
    <nav className="fixed inset-x-0 bottom-0 z-50 border-t border-line bg-surface/95 shadow-[0_-10px_30px_rgba(0,0,0,0.06)] backdrop-blur transition-colors duration-200 lg:bottom-auto lg:top-0 lg:border-b lg:border-t-0 lg:shadow-card">
      <div className="mx-auto flex max-w-6xl lg:h-14 lg:items-center lg:justify-between lg:px-6">
        <Link href="/feed" className="hidden text-lg font-extrabold ig-text lg:block">
          SnapGuide
        </Link>
        <div className="flex w-full lg:w-auto lg:gap-1">
        {NAV_ITEMS.map(({ href, labelKey, icon }) => {
          const active = pathname === href || pathname.startsWith(href + '/')
          return (
            <Link
              key={href}
              href={href}
              className={`flex flex-1 flex-col items-center gap-0.5 px-2 py-2 text-[10px] font-medium transition-colors lg:min-w-24 lg:flex-none lg:flex-row lg:justify-center lg:gap-2 lg:rounded-xl lg:px-3 lg:text-sm ${
                active ? 'text-accent lg:bg-accent-soft' : 'text-subtle hover:text-muted lg:hover:bg-surface-muted'
              }`}
            >
              <span className="text-lg leading-none lg:text-base">{icon}</span>
              {t(labelKey)}
            </Link>
          )
        })}
        </div>
      </div>
    </nav>
  )
}
