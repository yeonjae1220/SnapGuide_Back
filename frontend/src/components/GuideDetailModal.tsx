'use client'

import { useEffect, useState } from 'react'
import type { Guide } from '@/lib/types'

type Props = {
  guide: Guide
  onClose: () => void
}

export function GuideDetailModal({ guide, onClose }: Props) {
  const [idx, setIdx] = useState(0)
  const media = guide.media ?? []

  useEffect(() => {
    document.body.style.overflow = 'hidden'
    return () => { document.body.style.overflow = '' }
  }, [])

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

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/70 sm:items-center"
      onClick={onClose}
    >
      <div
        className="relative w-full max-w-lg overflow-hidden rounded-t-3xl bg-white sm:rounded-3xl"
        onClick={(e) => e.stopPropagation()}
      >
        <button
          onClick={onClose}
          className="absolute right-4 top-4 z-10 rounded-full bg-black/40 px-2 py-0.5 text-white"
        >
          ✕
        </button>

        {media.length > 0 && (
          <div className="relative aspect-[4/3] overflow-hidden bg-gray-100">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={media[idx].url}
              alt=""
              className="h-full w-full object-cover"
            />
            {media.length > 1 && (
              <>
                <button
                  onClick={prev}
                  className="absolute left-2 top-1/2 -translate-y-1/2 rounded-full bg-black/40 px-2 py-1 text-white"
                >
                  ‹
                </button>
                <button
                  onClick={next}
                  className="absolute right-2 top-1/2 -translate-y-1/2 rounded-full bg-black/40 px-2 py-1 text-white"
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
            <p className="mb-1 text-xs text-gray-400">@{guide.author.email}</p>
          )}
          {exifText && <p className="mb-2 text-xs text-gray-400">{exifText}</p>}
          <p className="mb-3 text-sm text-gray-800">{guide.tip}</p>
          {guide.locationPublic !== false &&
            (guide.locationName || guide.latitude != null) && (
              <p className="text-xs text-gray-500">
                📍{' '}
                {guide.locationName ??
                  `${guide.latitude?.toFixed(4)}°N, ${guide.longitude?.toFixed(4)}°E`}
              </p>
            )}
          <div className="mt-3 flex gap-3 text-xs text-gray-400">
            <span>조회 {guide.viewCount ?? 0}</span>
            <span>❤️ {guide.likeCount ?? 0}</span>
          </div>
        </div>
      </div>
    </div>
  )
}
