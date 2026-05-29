'use client'

export const dynamic = 'force-dynamic'

import { useEffect, useRef, useState } from 'react'
import { useRouter } from 'next/navigation'
import { api } from '@/lib/api'
import { useI18n } from '@/i18n/I18nProvider'
import { useAuthStore } from '@/stores/useAuthStore'

export default function UploadPage() {
  const { t } = useI18n()
  const router = useRouter()
  const accessToken = useAuthStore((s) => s.accessToken)

  const [tip, setTip] = useState('')
  const [locationPublic, setLocationPublic] = useState(true)
  const [files, setFiles] = useState<File[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const fileInputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (accessToken === null) router.replace('/')
  }, [accessToken, router])

  const handleSubmit = async () => {
    if (!files.length) return setError(t('upload.fileHint'))
    setError('')
    setLoading(true)

    navigator.geolocation?.getCurrentPosition(
      ({ coords }) => doUpload(coords.latitude, coords.longitude),
      () => doUpload(null, null),
    )
  }

  const doUpload = async (lat: number | null, lng: number | null) => {
    try {
      const form = new FormData()
      form.append('tip', tip)
      form.append('locationPublic', String(locationPublic))
      if (lat != null) form.append('latitude', String(lat))
      if (lng != null) form.append('longitude', String(lng))
      files.forEach((f) => form.append('files', f))

      await api.post('/guide/api/upload', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      router.replace('/guides')
    } catch {
      setError(t('common.error'))
    } finally {
      setLoading(false)
    }
  }

  if (!accessToken) return null

  return (
    <div className="mx-auto max-w-lg p-6">
      <h1 className="mb-6 text-2xl font-bold">{t('upload.title')}</h1>

      <div className="space-y-4 rounded-2xl border border-gray-100 bg-white p-5 shadow-sm">
        {/* file input */}
        <div
          onClick={() => fileInputRef.current?.click()}
          className="flex h-40 cursor-pointer flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed border-gray-200 text-gray-400 hover:border-pink-300 hover:text-pink-400 transition"
        >
          {files.length > 0 ? (
            <span className="text-sm font-medium text-gray-700">
              {files.length}개 선택됨
            </span>
          ) : (
            <>
              <span className="text-3xl">📷</span>
              <span className="text-sm">{t('upload.fileHint')}</span>
            </>
          )}
        </div>
        <input
          ref={fileInputRef}
          type="file"
          multiple
          accept="image/*,video/*"
          className="hidden"
          onChange={(e) => setFiles(Array.from(e.target.files ?? []))}
        />

        {/* tip */}
        <div>
          <label className="mb-1 block text-xs font-medium text-gray-500">
            {t('upload.tipLabel')}
          </label>
          <textarea
            value={tip}
            onChange={(e) => setTip(e.target.value)}
            placeholder={t('upload.tipPlaceholder')}
            rows={3}
            className="w-full resize-none rounded-xl border border-gray-200 px-4 py-3 text-sm outline-none focus:border-pink-400 focus:ring-2 focus:ring-pink-100"
          />
        </div>

        {/* location toggle */}
        <div className="flex items-center justify-between">
          <span className="text-sm text-gray-700">{t('upload.locationToggle')}</span>
          <button
            onClick={() => setLocationPublic((v) => !v)}
            className={`rounded-full px-4 py-1.5 text-xs font-semibold transition ${
              locationPublic
                ? 'bg-green-100 text-green-700'
                : 'bg-gray-100 text-gray-500'
            }`}
          >
            {locationPublic ? t('upload.locationOn') : t('upload.locationOff')}
          </button>
        </div>

        {error && <p className="text-xs text-red-500">{error}</p>}

        <button
          onClick={handleSubmit}
          disabled={loading}
          className="ig-bg w-full rounded-xl py-3 text-sm font-semibold text-white transition-opacity hover:opacity-90 disabled:opacity-60"
        >
          {loading ? t('common.loading') : t('upload.submit')}
        </button>
      </div>
    </div>
  )
}
