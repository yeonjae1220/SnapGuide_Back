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
import Link from 'next/link'

export default function GuidesPage() {
  const { t } = useI18n()
  const accessToken = useAuthStore((s) => s.accessToken)
  const initialized = useAuthStore((s) => s.initialized)
  const router = useRouter()
  const [guides, setGuides] = useState<Guide[]>([])
  const [selected, setSelected] = useState<Guide | null>(null)

  const [loading, setLoading] = useState(true)
  // 🔴 실패를 빈 배열로 치환하면 '조회 실패'와 '가이드 없음'이 화면에서 똑같아진다.
  //    실패 판정은 error 객체의 truthy 여부가 아니라 별도 boolean 으로 한다 —
  //    Promise.reject() 처럼 원인이 falsy 한 실패가 조용히 성공으로 취급된다 (GLOBAL-PIT-108).
  const [failed, setFailed] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    if (!initialized) return
    if (accessToken === null) {
      router.replace('/')
      return
    }
    let alive = true
    setLoading(true)
    setFailed(false)
    api
      .get('/guide/api/my')
      .then(({ data }) => {
        if (!alive) return
        setGuides(Array.isArray(data) ? data : [])
      })
      .catch((e) => {
        if (!alive) return
        console.error('[guides] 내 가이드 목록 조회 실패', e)
        setFailed(true)
      })
      .finally(() => {
        if (alive) setLoading(false)
      })
    return () => {
      alive = false
    }
  }, [accessToken, initialized, router, reloadKey])

  if (!initialized) return <p className="py-12 text-center text-sm text-subtle">{t('common.loading')}</p>
  if (!accessToken) return null

  return (
    <div className="p-4">
      <h1 className="mb-4 text-xl font-bold text-ink">{t('nav.myGuides')}</h1>
      {loading ? (
        <p className="py-12 text-center text-sm text-subtle">{t('common.loading')}</p>
      ) : failed ? (
        // 실패 시에는 목록·빈 상태 어느 쪽도 그리지 않는다 — 둘 다 거짓 정보가 된다.
        <div role="alert" className="rounded-2xl border border-line bg-surface p-6 text-center shadow-card">
          <p className="text-sm font-medium text-ink">{t('common.error')}</p>
          <button
            type="button"
            onClick={() => setReloadKey((k) => k + 1)}
            className="mt-4 inline-flex min-h-10 items-center justify-center rounded-xl bg-accent px-4 text-sm font-semibold text-white transition hover:opacity-90"
          >
            {t('common.retry')}
          </button>
        </div>
      ) : guides.length === 0 ? (
        <div className="rounded-2xl border border-line bg-surface p-6 text-center shadow-card">
          <p className="text-sm font-medium text-ink">{t('guide.empty')}</p>
          <p className="mt-1 text-xs text-muted">{t('guide.emptyAction')}</p>
          <Link
            href="/upload"
            className="mt-4 inline-flex min-h-10 items-center justify-center rounded-xl bg-accent px-4 text-sm font-semibold text-white transition hover:opacity-90"
          >
            {t('nav.upload')}
          </Link>
        </div>
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
