const CACHE_NAME = 'snapguide-static-v2'
const STATIC_URLS = [
  '/manifest.webmanifest',
  '/icon',
  '/icons/pwa-192x192.png',
  '/icons/pwa-512x512.png',
  '/images/snapguide-hero.webp',
]

function isCacheableStaticRequest(request) {
  if (request.method !== 'GET') return false

  const url = new URL(request.url)
  if (url.origin !== self.location.origin) return false
  if (url.search) return false
  if (request.mode === 'navigate' || request.destination === 'document') return false
  if (url.pathname.startsWith('/api/') || url.pathname.startsWith('/guide/api/')) return false
  if (url.pathname.startsWith('/oauth2/')) return false

  return (
    url.pathname.startsWith('/_next/static/') ||
    url.pathname.startsWith('/icons/') ||
    url.pathname.startsWith('/images/') ||
    url.pathname === '/manifest.webmanifest' ||
    url.pathname === '/icon'
  )
}

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(STATIC_URLS)).catch(() => undefined),
  )
  self.skipWaiting()
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key)))),
  )
  self.clients.claim()
})

self.addEventListener('fetch', (event) => {
  const request = event.request
  if (!isCacheableStaticRequest(request)) return

  event.respondWith(
    fetch(request)
      .then((response) => {
        if (!response.ok || response.type !== 'basic') return response
        const copy = response.clone()
        caches.open(CACHE_NAME).then((cache) => cache.put(request, copy)).catch(() => undefined)
        return response
      })
      .catch(() => caches.match(request).then((cached) => cached || Response.error())),
  )
})
