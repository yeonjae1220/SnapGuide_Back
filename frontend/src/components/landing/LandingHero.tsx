'use client'

import { useI18n } from '@/i18n/I18nProvider'

export function LandingHero() {
  const { t } = useI18n()

  return (
    <div className="relative hidden flex-1 flex-col justify-center overflow-hidden bg-black px-14 py-20 lg:flex">
      {/* gradient orbs */}
      <div className="pointer-events-none absolute -right-32 -top-32 h-[560px] w-[560px] animate-pulse rounded-full bg-[radial-gradient(circle,rgba(240,148,51,.45)_0%,rgba(188,24,136,.2)_55%,transparent_72%)]" />
      <div className="pointer-events-none absolute -bottom-24 -left-24 h-[420px] w-[420px] rounded-full bg-[radial-gradient(circle,rgba(0,149,246,.3)_0%,rgba(204,35,102,.18)_55%,transparent_72%)]" />

      <div className="relative z-10">
        <div className="mb-6 text-3xl font-bold ig-text">{t('landing.title')}</div>
        <p className="mb-10 whitespace-pre-line text-5xl font-extrabold leading-tight text-white">
          {t('landing.tagline')}
        </p>
        <ul className="space-y-5">
          {(
            [
              ['📍', 'landing.feat1.title', 'landing.feat1.desc'],
              ['📖', 'landing.feat2.title', 'landing.feat2.desc'],
              ['📷', 'landing.feat3.title', 'landing.feat3.desc'],
            ] as const
          ).map(([icon, titleKey, descKey]) => (
            <li key={titleKey} className="flex items-start gap-4">
              <span className="mt-0.5 text-2xl">{icon}</span>
              <div>
                <div className="font-semibold text-white">{t(titleKey)}</div>
                <div className="text-sm text-gray-400">{t(descKey)}</div>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}
