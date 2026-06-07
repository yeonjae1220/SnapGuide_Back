'use client'

import { useState } from 'react'
import Image from 'next/image'
import { api } from '@/lib/api'
import { useAuthStore } from '@/stores/useAuthStore'
import { useI18n } from '@/i18n/I18nProvider'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import type { Guide } from '@/lib/types'

function formatCoordinate(value: number, positive: string, negative: string): string {
  const direction = value >= 0 ? positive : negative
  return `${Math.abs(value).toFixed(4)}°${direction}`
}

function formatLocation(g: Guide): string {
  if (g.locationPublic === false) return ''
  const name = g.locationName ?? ''
  const coord =
    g.latitude != null && g.longitude != null
      ? `${formatCoordinate(g.latitude, 'N', 'S')}, ${formatCoordinate(g.longitude, 'E', 'W')}`
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

function isVideoUrl(url: string): boolean {
  return /\.(mp4|mov|webm|m4v)(\?|#|$)/i.test(url)
}

type Props = {
  guide: Guide
  onOpen: (g: Guide) => void
  onDeleted?: (id: number) => void
  onHover?: (id: number | null) => void
  highlighted?: boolean
}

export function GuideCard({ guide: initial, onOpen, onDeleted, onHover, highlighted }: Props) {
  const { t } = useI18n()
  const { accessToken, email } = useAuthStore()
  const [guide, setGuide] = useState(initial)
  const [actionError, setActionError] = useState('')
  const [menuOpen, setMenuOpen] = useState(false)
  const [editing, setEditing] = useState(false)
  const [draftTip, setDraftTip] = useState(initial.tip)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [saving, setSaving] = useState(false)

  const isOwner = !!accessToken && !!guide.author?.email && guide.author.email === email

  const handleLike = async (e: React.MouseEvent) => {
    e.stopPropagation()
    if (!accessToken) {
      setActionError(t('guide.likeLoginRequired'))
      return
    }
    setActionError('')
    try {
      const { data } = await api.post(`/guide/api/like/${guide.id}`)
      setGuide((g) => ({ ...g, userHasLiked: data.liked, likeCount: data.likeCount }))
    } catch {
      setActionError(t('common.error'))
    }
  }

  const handleEdit = async () => {
    const nextTip = draftTip.trim()
    if (!nextTip) return
    setActionError('')
    setSaving(true)
    try {
      await api.put(`/guide/api/${guide.id}`, { tip: nextTip })
      setGuide((g) => ({ ...g, tip: nextTip }))
      setEditing(false)
      setMenuOpen(false)
    } catch {
      setActionError(t('common.error'))
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async () => {
    setActionError('')
    setSaving(true)
    try {
      await api.delete(`/guide/api/${guide.id}`)
      setConfirmDelete(false)
      onDeleted?.(guide.id)
    } catch {
      setActionError(t('common.error'))
    } finally {
      setSaving(false)
    }
  }

  const firstImg = guide.media?.[0]?.url
  const isMulti = (guide.media?.length ?? 0) > 1
  const location = formatLocation(guide)
  const exif = formatExif(guide)

  return (
    <>
      <article
        data-guide-card={guide.id}
        onClick={() => onOpen(guide)}
        onMouseEnter={() => onHover?.(guide.id)}
        onMouseLeave={() => onHover?.(null)}
        className={`cursor-pointer overflow-hidden rounded-2xl border bg-surface shadow-card transition duration-200 hover:-translate-y-0.5 ${
          highlighted ? '-translate-y-0.5 border-accent ring-2 ring-accent/30' : 'border-line hover:border-accent/30'
        }`}
      >
        {firstImg ? (
          <div className="relative aspect-[4/3] bg-surface-muted">
            {isVideoUrl(firstImg) ? (
              <video src={firstImg} className="h-full w-full object-cover" muted playsInline preload="metadata" />
            ) : (
              <Image
                src={firstImg}
                alt=""
                fill
                unoptimized
                sizes="(min-width: 1024px) 420px, (min-width: 640px) 50vw, 100vw"
                className="object-cover"
              />
            )}
            {isMulti && (
              <span className="absolute right-2 top-2 rounded-full bg-black/60 px-2 py-0.5 text-[10px] text-white">
                +{guide.media!.length - 1}
              </span>
            )}
          </div>
        ) : (
          <div className="flex aspect-[4/3] items-center justify-center bg-surface-muted text-3xl" aria-hidden="true">
            📷
          </div>
        )}
        <div className="p-3">
          <p className="mb-1 line-clamp-2 text-sm font-medium leading-5 text-ink">{guide.tip}</p>
          {exif && <p className="mb-1 line-clamp-1 text-xs text-subtle">{exif}</p>}
          {location && (
            <p className="mb-2 line-clamp-1 text-xs text-muted">
              <span aria-hidden="true">📍</span> {location}
            </p>
          )}
          {guide.locationPublic === false && (
            <p className="mb-2 text-xs text-subtle">{t('guide.locationPrivate')}</p>
          )}
          {actionError && (
            <p className="mb-1 text-xs text-danger">{actionError}</p>
          )}
          <div className="flex items-center justify-between gap-3">
            <span className="text-xs text-subtle">
              {t('guide.views')} {guide.viewCount ?? 0}
            </span>
            <div className="flex items-center gap-1">
              <button
                type="button"
                onClick={handleLike}
                className="flex min-h-10 items-center gap-1 rounded-full px-3 text-sm transition hover:bg-surface-muted"
                aria-label={t('guide.likeLoginRequired')}
              >
                <span aria-hidden="true">{guide.userHasLiked ? '❤️' : '🤍'}</span>
                <span className="text-xs font-medium text-muted">{guide.likeCount ?? 0}</span>
              </button>
              {isOwner && (
                <div className="relative">
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation()
                      setMenuOpen((v) => !v)
                    }}
                    className="flex h-10 w-10 items-center justify-center rounded-full text-lg text-muted transition hover:bg-surface-muted hover:text-ink"
                    aria-label={t('guide.moreActions')}
                    aria-expanded={menuOpen}
                  >
                    ⋯
                  </button>
                  {menuOpen && (
                    <div
                      className="absolute bottom-full right-0 z-20 mb-1 w-28 overflow-hidden rounded-xl border border-line bg-surface-elevated shadow-card"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <button
                        type="button"
                        onClick={() => {
                          setDraftTip(guide.tip)
                          setEditing(true)
                        }}
                        className="block min-h-10 w-full px-3 text-left text-sm text-ink transition hover:bg-surface-muted"
                      >
                        {t('guide.edit')}
                      </button>
                      <button
                        type="button"
                        onClick={() => setConfirmDelete(true)}
                        className="block min-h-10 w-full px-3 text-left text-sm text-danger transition hover:bg-danger-soft"
                      >
                        {t('guide.delete')}
                      </button>
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      </article>

      {editing && (
        <div className="fixed inset-0 z-[60] flex items-end justify-center bg-overlay/60 px-3 pb-3 sm:items-center sm:pb-0" onClick={() => setEditing(false)}>
          <div className="w-full max-w-md rounded-2xl border border-line bg-surface p-4 shadow-card" onClick={(e) => e.stopPropagation()}>
            <h2 className="text-base font-bold text-ink">{t('guide.edit')}</h2>
            <textarea
              value={draftTip}
              onChange={(e) => setDraftTip(e.target.value)}
              rows={4}
              className="mt-3 w-full resize-none rounded-xl border border-line bg-field px-4 py-3 text-sm text-ink outline-none transition-colors focus:border-accent/60 focus:ring-2 focus:ring-accent/15"
            />
            <div className="mt-4 grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => setEditing(false)}
                className="min-h-11 rounded-xl border border-line text-sm font-semibold text-muted transition hover:bg-surface-muted"
              >
                {t('common.cancel')}
              </button>
              <button
                type="button"
                onClick={handleEdit}
                disabled={saving || !draftTip.trim()}
                className="min-h-11 rounded-xl bg-accent text-sm font-semibold text-white transition hover:opacity-90 disabled:opacity-50"
              >
                {saving ? t('common.loading') : t('guide.saveEdit')}
              </button>
            </div>
          </div>
        </div>
      )}

      <ConfirmDialog
        open={confirmDelete}
        title={t('guide.delete')}
        description={t('guide.deleteConfirm')}
        confirmLabel={saving ? t('common.loading') : t('guide.delete')}
        cancelLabel={t('common.cancel')}
        loading={saving}
        danger
        onCancel={() => setConfirmDelete(false)}
        onConfirm={handleDelete}
      />
    </>
  )
}
