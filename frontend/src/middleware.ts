import { NextResponse } from 'next/server'
import type { NextRequest } from 'next/server'

export function middleware(request: NextRequest) {
  const nonce = Buffer.from(crypto.randomUUID()).toString('base64')

  const csp = [
    "default-src 'self'",
    // Google Maps JS SDK는 domain-based allowlist로 커버 (nonce 불가 — 외부 스크립트)
    `script-src 'self' 'nonce-${nonce}' https://maps.googleapis.com`,
    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
    "img-src 'self' data: https: blob:",
    "font-src 'self' https://fonts.gstatic.com",
    "connect-src 'self' https://maps.googleapis.com https://maps.gstatic.com https://snapguide.mungji.com",
    "frame-src 'none'",
    "object-src 'none'",
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
