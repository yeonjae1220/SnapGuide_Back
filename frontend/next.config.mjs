/** @type {import('next').NextConfig} */
// CSP는 middleware.ts에서 nonce 기반으로 생성
const securityHeaders = [
  { key: 'X-Frame-Options', value: 'DENY' },
  { key: 'X-Content-Type-Options', value: 'nosniff' },
  { key: 'Referrer-Policy', value: 'strict-origin-when-cross-origin' },
  { key: 'Permissions-Policy', value: 'camera=(), microphone=(), geolocation=(self)' },
]

const nextConfig = {
  output: 'standalone',
  async redirects() {
    return [
      { source: '/favicon.ico', destination: '/icon', permanent: false },
    ]
  },
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
      {
        source: '/media/:path*',
        destination: `${process.env.API_URL ?? 'http://localhost:8080'}/media/:path*`,
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
