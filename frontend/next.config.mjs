/** @type {import('next').NextConfig} */
const securityHeaders = [
  { key: 'X-Frame-Options', value: 'DENY' },
  { key: 'X-Content-Type-Options', value: 'nosniff' },
  { key: 'Referrer-Policy', value: 'strict-origin-when-cross-origin' },
  { key: 'Permissions-Policy', value: 'camera=(), microphone=(), geolocation=()' },
  {
    key: 'Content-Security-Policy',
    value: [
      "default-src 'self'",
      // Next App Router injects inline bootstrap data scripts for hydration.
      "script-src 'self' 'unsafe-inline'",
      "style-src 'self' 'unsafe-inline'",
      "img-src 'self' data: https:",
      "font-src 'self'",
      "connect-src 'self' https://snapguide.mungji.com",
      "frame-src 'none'",
      "object-src 'none'",
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
