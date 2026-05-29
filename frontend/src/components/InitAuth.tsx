'use client'

import { Suspense, useEffect, useRef } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { api } from '@/lib/api'
import { useAuthStore } from '@/stores/useAuthStore'

function InitAuthInner() {
  const { setTokens, accessToken, clearTokens } = useAuthStore()
  const router = useRouter()
  const searchParams = useSearchParams()
  const initialized = useRef(false)

  useEffect(() => {
    if (initialized.current) return
    initialized.current = true

    const code = searchParams.get('code')

    if (code) {
      api
        .post('/api/auth/oauth/token', { code })
        .then(({ data }) => {
          setTokens(data.accessToken, data.email)
          router.replace('/feed')
        })
        .catch(() => router.replace('/'))
      return
    }

    if (!accessToken) {
      api
        .post('/api/auth/reissue', { accessToken: '' })
        .then(({ data }) => {
          setTokens(data.accessToken)
          api
            .get('/api/auth/test')
            .then((r) => {
              const email = r.data?.replace('인증된 사용자: ', '') ?? ''
              setTokens(data.accessToken, email)
            })
            .catch(() => {})
        })
        .catch(() => clearTokens())
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  return null
}

export function InitAuth() {
  return (
    <Suspense fallback={null}>
      <InitAuthInner />
    </Suspense>
  )
}
