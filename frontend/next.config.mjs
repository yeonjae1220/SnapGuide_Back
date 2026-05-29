/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone',
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: `${process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'}/api/:path*`,
      },
      {
        source: '/guide/api/:path*',
        destination: `${process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'}/guide/api/:path*`,
      },
      {
        source: '/oauth2/:path*',
        destination: `${process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'}/oauth2/:path*`,
      },
      {
        source: '/login/:path*',
        destination: `${process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'}/login/:path*`,
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
