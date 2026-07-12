'use client'

import { useI18n } from '@/i18n/I18nProvider'

export function LandingHero() {
  const { t } = useI18n()

  return (
    <div className="relative hidden flex-1 flex-col justify-end overflow-hidden bg-[#0b0b0d] px-14 py-20 lg:flex">
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        src="/images/snapguide-hero.webp"
        alt=""
        className="absolute inset-0 h-full w-full object-cover opacity-75"
      />
      <div className="absolute inset-0 bg-[linear-gradient(90deg,rgba(8,8,10,.86),rgba(8,8,10,.38)_48%,rgba(8,8,10,.18))]" />

      <div className="relative z-10 max-w-2xl">
        <div className="mb-6 text-3xl font-bold ig-text">{t('landing.title')}</div>
        <h1 className="mb-10 whitespace-pre-line text-5xl font-extrabold leading-tight text-white">
          {t('landing.tagline')}
        </h1>
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
                <div className="text-sm text-zinc-400">{t(descKey)}</div>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}
