'use client'

import { useState } from 'react'
import { api } from '@/lib/api'
import { useAuthStore } from '@/stores/useAuthStore'
import { useI18n } from '@/i18n/I18nProvider'
import type { Guide } from '@/lib/types'

function formatLocation(g: Guide): string {
  if (g.locationPublic === false) return ''
  const name = g.locationName ?? ''
  const coord =
    g.latitude != null && g.longitude != null
      ? `${g.latitude.toFixed(4)}°N, ${g.longitude.toFixed(4)}°E`
      : ''
  if (name && coord) return `${name} (${coord})`
  return name || coord
}

function formatExif(g: Guide): string {
  const exif = g.media?.[0]?.exif
  if (!exif) return ''
  return [
    exif.model ?? exif.manufacturer,
    exif.iso ? `ISO ${exif.iso}` : '',
    exif.aperture,
    exif.shutterSpeed,
    exif.focalLength ? `${exif.focalLength}mm` : '',
  ]
    .filter(Boolean)
    .join(' · ')
}

type Props = {
  guide: Guide
  onOpen: (g: Guide) => void
  onDeleted?: (id: number) => void
}

export function GuideCard({ guide: initial, onOpen, onDeleted }: Props) {
  const { t } = useI18n()
  const accessToken = useAuthStore((s) => s.accessToken)
  const [guide, setGuide] = useState(initial)

  const handleLike = async (e: React.MouseEvent) => {
    e.stopPropagation()
    if (!accessToken) return alert(t('guide.likeLoginRequired'))
    try {
      const { data } = await api.post(`/guide/api/like/${guide.id}`)
      setGuide((g) => ({ ...g, userHasLiked: data.liked, likeCount: data.likeCount }))
    } catch {}
  }

  const handleEdit = async (e: React.MouseEvent) => {
    e.stopPropagation()
    const newTip = prompt(t('guide.editPrompt'), guide.tip)
    if (!newTip) return
    try {
      await api.put(`/guide/api/${guide.id}`, { tip: newTip })
      setGuide((g) => ({ ...g, tip: newTip }))
    } catch {}
  }

  const handleDelete = async (e: React.MouseEvent) => {
    e.stopPropagation()
    if (!confirm(t('guide.deleteConfirm'))) return
    try {
      await api.delete(`/guide/api/${guide.id}`)
      onDeleted?.(guide.id)
    } catch {}
  }

  const firstImg = guide.media?.[0]?.url
  const isMulti = (guide.media?.length ?? 0) > 1
  const location = formatLocation(guide)
  const exif = formatExif(guide)

  return (
    <div
      onClick={() => onOpen(guide)}
      className="cursor-pointer overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-sm transition hover:shadow-md"
    >
      {firstImg && (
        <div className="relative aspect-[4/3] bg-gray-100">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={firstImg} alt="" className="h-full w-full object-cover" />
          {isMulti && (
            <span className="absolute right-2 top-2 rounded-full bg-black/60 px-2 py-0.5 text-[10px] text-white">
              +{guide.media!.length - 1}
            </span>
          )}
        </div>
      )}
      <div className="p-3">
        <p className="mb-1 line-clamp-2 text-sm text-gray-800">{guide.tip}</p>
        {exif && <p className="mb-1 text-xs text-gray-400">{exif}</p>}
        {location && (
          <p className="mb-2 flex items-center gap-1 text-xs text-gray-500">
            <span>📍</span> {location}
          </p>
        )}
        {guide.locationPublic === false && (
          <p className="mb-2 text-xs text-gray-400">{t('guide.locationPrivate')}</p>
        )}
        <div className="flex items-center justify-between">
          <span className="text-xs text-gray-400">
            {t('guide.views')} {guide.viewCount ?? 0}
          </span>
          <div className="flex items-center gap-2">
            <button
              onClick={handleLike}
              className="flex items-center gap-1 text-sm"
            >
              {guide.userHasLiked ? '❤️' : '🤍'}
              <span className="text-xs text-gray-500">{guide.likeCount ?? 0}</span>
            </button>
            {accessToken && (
              <>
                <button
                  onClick={handleEdit}
                  className="text-xs text-gray-400 hover:text-blue-500"
                >
                  {t('guide.edit')}
                </button>
                <button
                  onClick={handleDelete}
                  className="text-xs text-gray-400 hover:text-red-500"
                >
                  {t('guide.delete')}
                </button>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
