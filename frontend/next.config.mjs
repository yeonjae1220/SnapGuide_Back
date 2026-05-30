/** @type {import('next').NextConfig} */
const securityHeaders = [
  { key: 'X-Frame-Options', value: 'DENY' },
  { key: 'X-Content-Type-Options', value: 'nosniff' },
  { key: 'Referrer-Policy', value: 'strict-origin-when-cross-origin' },
  // geolocation=(self): Feed 페이지의 "내 위치" 버튼에서 navigator.geolocation 사용
  { key: 'Permissions-Policy', value: 'camera=(), microphone=(), geolocation=(self)' },
  {
    key: 'Content-Security-Policy',
    value: [
      "default-src 'self'",
      // Next App Router hydration 인라인 스크립트 + Google Maps JS SDK
      "script-src 'self' 'unsafe-inline' https://maps.googleapis.com",
      "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
      // 이미지: 지도 타일, 가이드 업로드 이미지, data URIs
      "img-src 'self' data: https: blob:",
      "font-src 'self' https://fonts.gstatic.com",
      // Google Maps API XHR/fetch 호출 + 앱 API
      "connect-src 'self' https://maps.googleapis.com https://maps.gstatic.com https://snapguide.mungji.com",
      // Google Maps에서 일부 기능이 iframe 사용
      "frame-src 'none'",
      "object-src 'none'",
      // Google Maps 워커 스크립트
      "worker-src blob:",
    ].join('; '),
  },
]

const nextConfig = {
  output: 'standalone',
  async headers() {
    return [{ source: '/(.*)', headers: securityHeaders }]
  },
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: `${process.env.API_URL ?? 'http://localhost:8080'}/api/:path*`,
      },
      {
        source: '/guide/api/:path*',
        destination: `${process.env.API_URL ?? 'http://localhost:8080'}/guide/api/:path*`,
      },
      {
        source: '/oauth2/:path*',
        destination: `${process.env.API_URL ?? 'http://localhost:8080'}/oauth2/:path*`,
      },
      {
        source: '/login/:path*',
        destination: `${process.env.API_URL ?? 'http://localhost:8080'}/login/:path*`,
      },
    ]
  },
  images: {
    remotePatterns: [
      { protocol: 'https', hostname: 'snapguide.mungji.com' },
      { protocol: 'http', hostname: 'localhost' },
    ],
  },
}

export default nextConfig
