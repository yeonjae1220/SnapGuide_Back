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
    if (accessToken === null) {
      router.replace('/')
      return
    }
    api
      .get('/guide/api/my')
      .then(({ data }) => setGuides(data))
      .catch(() => setGuides([]))
      .finally(() => setLoading(false))
  }, [accessToken, router])

  if (!accessToken) return null

  return (
    <div className="p-4">
      <h1 className="mb-4 text-xl font-bold text-ink">{t('nav.myGuides')}</h1>
      {loading ? (
        <p className="py-12 text-center text-sm text-subtle">{t('common.loading')}</p>
      ) : guides.length === 0 ? (
        <p className="py-12 text-center text-sm text-subtle">{t('guide.empty')}</p>
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
