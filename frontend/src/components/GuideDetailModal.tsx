'use client'

import { useEffect, useState } from 'react'
import Image from 'next/image'
import { useI18n } from '@/i18n/I18nProvider'
import type { Guide } from '@/lib/types'

type Props = {
  guide: Guide
  onClose: () => void
}

function isVideoUrl(url: string): boolean {
  return /\.(mp4|mov|webm|m4v)(\?|#|$)/i.test(url)
}

export function GuideDetailModal({ guide, onClose }: Props) {
  const { t } = useI18n()
  const [idx, setIdx] = useState(0)
  const media = guide.media ?? []

  useEffect(() => {
    document.body.style.overflow = 'hidden'
    const handleKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', handleKey)
    return () => {
      document.body.style.overflow = ''
      window.removeEventListener('keydown', handleKey)
    }
  }, [onClose])

  const prev = () => setIdx((i) => (i - 1 + media.length) % media.length)
  const next = () => setIdx((i) => (i + 1) % media.length)

  const exif = media[idx]?.exif
  const exifText = exif
    ? [
        exif.model ?? exif.manufacturer,
        exif.iso ? `ISO ${exif.iso}` : '',
        exif.aperture,
        exif.shutterSpeed,
        exif.focalLength ? `${exif.focalLength}mm` : '',
      ]
        .filter(Boolean)
        .join(' · ')
    : ''

  const locationText =
    guide.locationName ??
    (guide.latitude != null && guide.longitude != null
      ? `${Math.abs(guide.latitude).toFixed(4)}°${guide.latitude >= 0 ? 'N' : 'S'}, ${Math.abs(guide.longitude).toFixed(4)}°${guide.longitude >= 0 ? 'E' : 'W'}`
      : '')

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-overlay/70 backdrop-blur-sm sm:items-center"
      onClick={onClose}
    >
      <div
        role="dialog"
        aria-modal="true"
        className="relative w-full max-w-lg overflow-hidden rounded-t-3xl border border-line bg-surface shadow-card transition-colors duration-200 sm:rounded-3xl"
        onClick={(e) => e.stopPropagation()}
      >
        <button
          onClick={onClose}
          aria-label={t('common.cancel')}
          className="absolute right-3 top-3 z-10 flex h-11 w-11 items-center justify-center rounded-full bg-black/45 text-lg text-white backdrop-blur transition hover:bg-black/60"
        >
          ✕
        </button>

        {media.length > 0 && (
          <div className="relative aspect-[4/3] overflow-hidden bg-surface-muted">
            {isVideoUrl(media[idx].url) ? (
              <video src={media[idx].url} controls className="h-full w-full object-cover" />
            ) : (
              <Image
                src={media[idx].url}
                alt=""
                fill
                unoptimized
                sizes="(min-width: 640px) 512px, 100vw"
                className="object-cover"
              />
            )}
            {media.length > 1 && (
              <>
                <button
                  onClick={prev}
                  aria-label="Previous media"
                  className="absolute left-2 top-1/2 flex h-11 w-11 -translate-y-1/2 items-center justify-center rounded-full bg-black/45 text-2xl text-white backdrop-blur transition hover:bg-black/60"
                >
                  ‹
                </button>
                <button
                  onClick={next}
                  aria-label="Next media"
                  className="absolute right-2 top-1/2 flex h-11 w-11 -translate-y-1/2 items-center justify-center rounded-full bg-black/45 text-2xl text-white backdrop-blur transition hover:bg-black/60"
                >
                  ›
                </button>
                <div className="absolute bottom-2 left-1/2 flex -translate-x-1/2 gap-1">
                  {media.map((_, i) => (
                    <div
                      key={i}
                      className={`h-1.5 w-1.5 rounded-full transition-colors ${
                        i === idx ? 'bg-white' : 'bg-white/40'
                      }`}
                    />
                  ))}
                </div>
              </>
            )}
          </div>
        )}

        <div className="p-5">
          {guide.author?.email && (
            <p className="mb-1 text-xs text-subtle">@{guide.author.email}</p>
          )}
          {exifText && <p className="mb-2 text-xs text-subtle">{exifText}</p>}
          <p className="mb-3 whitespace-pre-wrap text-sm leading-6 text-ink">{guide.tip}</p>
          {guide.locationPublic !== false &&
            locationText && (
              <p className="text-xs text-muted">
                📍 {locationText}
              </p>
            )}
          <div className="mt-3 flex gap-3 text-xs text-subtle">
            <span>조회 {guide.viewCount ?? 0}</span>
            <span>❤️ {guide.likeCount ?? 0}</span>
          </div>
        </div>
      </div>
    </div>
  )
}
