import type { MetadataRoute } from 'next'

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: 'SnapGuide',
    short_name: 'SnapGuide',
    description: '여행의 순간을 가이드로, 세상과 나누다.',
    start_url: '/feed',
    display: 'standalone',
    background_color: '#ffffff',
    theme_color: '#bc1888',
    icons: [
      { src: '/icon', sizes: '32x32', type: 'image/png' },
      { src: '/icons/pwa-192x192.png', sizes: '192x192', type: 'image/png', purpose: 'any' },
      { src: '/icons/pwa-192x192.png', sizes: '192x192', type: 'image/png', purpose: 'maskable' },
      { src: '/icons/pwa-512x512.png', sizes: '512x512', type: 'image/png', purpose: 'any' },
      { src: '/icons/pwa-512x512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
    ],
  }
}
