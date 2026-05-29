'use client'

export const dynamic = 'force-dynamic'


import { useEffect, useState } from 'react'
import { api } from '@/lib/api'
import { useAuthStore } from '@/stores/useAuthStore'
import { useI18n } from '@/i18n/I18nProvider'
import { GuideCard } from '@/components/GuideCard'
import { GuideDetailModal } from '@/components/GuideDetailModal'
import type { Guide } from '@/lib/types'
import { useRouter } from 'next/navigation'

export default function GuidesPage() {
  const { t } = useI18n()
  const accessToken = useAuthStore((s) => s.accessToken)
  const router = useRouter()
  const [guides, setGuides] = useState<Guide[]>([])
  const [selected, setSelected] = useState<Guide | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!accessToken) {
      setLoading(false)
      return
    }
    api
      .get('/guide/api/my')
      .then(({ data }) => setGuides(data))
      .catch(() => setGuides([]))
      .finally(() => setLoading(false))
  }, [accessToken])

  if (!accessToken) {
    return (
      <div className="flex h-[60vh] flex-col items-center justify-center gap-4">
        <p className="text-gray-500">{t('nav.login')}</p>
        <button
          onClick={() => router.replace('/')}
          className="ig-bg rounded-xl px-6 py-2 text-sm font-semibold text-white"
        >
          {t('nav.login')}
        </button>
      </div>
    )
  }

  return (
    <div className="p-4">
      <h1 className="mb-4 text-xl font-bold">{t('nav.myGuides')}</h1>
      {loading ? (
        <p className="py-12 text-center text-sm text-gray-400">{t('common.loading')}</p>
      ) : guides.length === 0 ? (
        <p className="py-12 text-center text-sm text-gray-400">{t('guide.empty')}</p>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {guides.map((g) => (
            <GuideCard
              key={g.id}
              guide={g}
              onOpen={setSelected}
              onDeleted={(id) => setGuides((gs) => gs.filter((x) => x.id !== id))}
            />
          ))}
        </div>
      )}
      {selected && <GuideDetailModal guide={selected} onClose={() => setSelected(null)} />}
    </div>
  )
}
