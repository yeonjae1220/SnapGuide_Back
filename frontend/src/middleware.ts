import { NextResponse } from 'next/server'
import type { NextRequest } from 'next/server'

export function middleware(request: NextRequest) {
  const nonce = Buffer.from(crypto.randomUUID()).toString('base64')

  const csp = [
    "default-src 'self'",
    // 'strict-dynamic'이 nonce를 통해 신뢰된 스크립트가 로드하는 하위 스크립트를 허용하며
    // 구형 브라우저 fallback을 위해 도메인 allowlist 유지 ('unsafe-inline'은 strict-dynamic 적용 시 무시되므로 제거)
    `script-src 'nonce-${nonce}' 'strict-dynamic' https://maps.googleapis.com`,
    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
    "img-src 'self' data: https: blob:",
    "font-src 'self' https://fonts.gstatic.com",
    "connect-src 'self' https://maps.googleapis.com https://maps.gstatic.com https://snapguide.mungji.com",
    "frame-src 'none'",
    "object-src 'none'",
    "base-uri 'self'",
    "worker-src blob:",
  ].join('; ')

  const requestHeaders = new Headers(request.headers)
  requestHeaders.set('x-nonce', nonce)

  const response = NextResponse.next({ request: { headers: requestHeaders } })
  response.headers.set('Content-Security-Policy', csp)
  return response
}

export const config = {
  matcher: [
    '/((?!_next/static|_next/image|favicon.ico|icons|manifest.json).*)',
  ],
}
