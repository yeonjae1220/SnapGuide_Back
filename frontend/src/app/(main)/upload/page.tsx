'use client'

export const dynamic = 'force-dynamic'

import { useEffect, useMemo, useRef, useState } from 'react'
import { useRouter } from 'next/navigation'
import { api } from '@/lib/api'
import { useI18n } from '@/i18n/I18nProvider'
import { useAuthStore } from '@/stores/useAuthStore'

type LocationMode = 'public' | 'private' | 'none'

export default function UploadPage() {
  const { t } = useI18n()
  const router = useRouter()
  const accessToken = useAuthStore((s) => s.accessToken)
  const initialized = useAuthStore((s) => s.initialized)

  const [tip, setTip] = useState('')
  const [locationMode, setLocationMode] = useState<LocationMode>('public')
  const [files, setFiles] = useState<File[]>([])
  const [loading, setLoading] = useState(false)
  const [progress, setProgress] = useState(0)
  const [processing, setProcessing] = useState(false)
  const [error, setError] = useState('')
  const fileInputRef = useRef<HTMLInputElement>(null)

  const previews = useMemo(
    () => files.map((file) => ({ file, url: URL.createObjectURL(file) })),
    [files],
  )

  useEffect(() => {
    return () => {
      previews.forEach((preview) => URL.revokeObjectURL(preview.url))
    }
  }, [previews])

  useEffect(() => {
    if (!initialized) return
    if (accessToken === null) router.replace('/')
  }, [accessToken, initialized, router])

  const addFiles = (selected: File[]) => {
    setError('')
    setFiles((current) => [...current, ...selected])
    if (fileInputRef.current) fileInputRef.current.value = ''
  }

  const removeFile = (index: number) => {
    setFiles((current) => current.filter((_, i) => i !== index))
  }

  const handleSubmit = async () => {
    if (!files.length) return setError(t('upload.fileHint'))
    setError('')
    setLoading(true)
    setProgress(0)

    if (locationMode === 'none' || !navigator.geolocation) {
      doUpload(null, null)
      return
    }

    navigator.geolocation.getCurrentPosition(
      ({ coords }) => doUpload(coords.latitude, coords.longitude),
      () => doUpload(null, null),
      { timeout: 3000, maximumAge: 60000 },
    )
  }

  const doUpload = async (lat: number | null, lng: number | null) => {
    try {
      const form = new FormData()
      form.append('tip', tip)
      form.append('locationPublic', String(locationMode === 'public'))
      if (lat != null) form.append('latitude', String(lat))
      if (lng != null) form.append('longitude', String(lng))
      files.forEach((f) => form.append('files', f))

      await api.post('/guide/api/upload', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (event) => {
          if (!event.total) return
          setProgress(Math.round((event.loaded / event.total) * 100))
        },
      })
      setProcessing(true)
      router.replace('/guides')
    } catch {
      setError(t('common.error'))
    } finally {
      setLoading(false)
    }
  }

  if (!initialized) return <p className="py-12 text-center text-sm text-subtle">{t('common.loading')}</p>
  if (!accessToken) return null

  return (
    <div className="mx-auto max-w-2xl p-4 sm:p-6">
      <div className="mb-6">
        <p className="text-xs font-semibold uppercase tracking-[0.08em] text-accent">SnapGuide</p>
        <h1 className="mt-1 text-2xl font-bold text-ink">{t('upload.title')}</h1>
      </div>

      <div className="space-y-5 rounded-2xl border border-line bg-surface p-4 shadow-card transition-colors duration-200 sm:p-5">
        <div>
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            className="flex min-h-44 w-full cursor-pointer flex-col items-center justify-center gap-3 rounded-2xl border-2 border-dashed border-line bg-surface-muted px-4 text-center text-subtle transition hover:border-accent/60 hover:text-accent"
          >
            <span className="text-3xl" aria-hidden="true">📷</span>
            <span className="text-sm font-medium">{files.length ? t('upload.addMore') : t('upload.fileHint')}</span>
          </button>
          <input
            ref={fileInputRef}
            id="guide-files"
            name="files"
            type="file"
            multiple
            accept="image/*,video/*"
            className="hidden"
            onChange={(e) => addFiles(Array.from(e.target.files ?? []))}
          />
        </div>

        {previews.length > 0 && (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
            {previews.map((preview, index) => (
              <div key={`${preview.file.name}-${index}`} className="group relative overflow-hidden rounded-xl border border-line bg-surface-muted">
                <div className="aspect-square">
                  {preview.file.type.startsWith('video/') ? (
                    <video src={preview.url} className="h-full w-full object-cover" muted />
                  ) : (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img src={preview.url} alt="" className="h-full w-full object-cover" />
                  )}
                </div>
                <button
                  type="button"
                  onClick={() => removeFile(index)}
                  className="absolute right-2 top-2 flex h-9 w-9 items-center justify-center rounded-full bg-black/55 text-white backdrop-blur transition hover:bg-black/70"
                  aria-label={t('upload.removeFile')}
                >
                  ✕
                </button>
                <p className="truncate px-2 py-1.5 text-xs text-muted">{preview.file.name}</p>
              </div>
            ))}
          </div>
        )}

        <div>
          <label htmlFor="guide-tip" className="mb-1 block text-xs font-medium text-muted">
            {t('upload.tipLabel')}
          </label>
          <textarea
            id="guide-tip"
            name="tip"
            value={tip}
            onChange={(e) => setTip(e.target.value)}
            placeholder={t('upload.tipPlaceholder')}
            rows={4}
            className="w-full resize-none rounded-xl border border-line bg-field px-4 py-3 text-sm text-ink outline-none transition-colors placeholder:text-subtle focus:border-accent/60 focus:ring-2 focus:ring-accent/15"
          />
        </div>

        <div>
          <p className="mb-2 text-sm font-medium text-ink">{t('upload.locationToggle')}</p>
          <div className="grid gap-2 sm:grid-cols-3">
            {(
              [
                ['public', t('upload.locationOn')],
                ['private', t('upload.locationOff')],
                ['none', t('upload.locationNone')],
              ] as const
            ).map(([value, label]) => (
              <button
                key={value}
                type="button"
                onClick={() => setLocationMode(value)}
                className={`min-h-11 rounded-xl border px-3 text-sm font-semibold transition ${
                  locationMode === value
                    ? 'border-accent bg-accent-soft text-accent'
                    : 'border-line bg-surface-muted text-muted hover:text-ink'
                }`}
              >
                {label}
              </button>
            ))}
          </div>
          <p className="mt-2 text-xs text-subtle">{t('upload.locationHelp')}</p>
        </div>

        {(loading || processing) && (
          <div className="rounded-xl bg-surface-muted p-3">
            <div className="mb-2 flex items-center justify-between text-xs text-muted">
              <span>{processing ? t('upload.processing') : t('upload.progress')}</span>
              <span>{processing ? '✓' : `${progress}%`}</span>
            </div>
            <div className="h-2 overflow-hidden rounded-full bg-line">
              <div className="h-full rounded-full bg-accent transition-all" style={{ width: `${processing ? 100 : progress}%` }} />
            </div>
          </div>
        )}

        {error && <p className="text-xs text-danger">{error}</p>}

        <button
          type="button"
          onClick={handleSubmit}
          disabled={loading || files.length === 0}
          className="ig-bg w-full min-h-12 rounded-xl text-sm font-semibold text-white transition-opacity hover:opacity-90 disabled:opacity-50"
        >
          {loading ? t('common.loading') : t('upload.submit')}
        </button>
      </div>
    </div>
  )
}
