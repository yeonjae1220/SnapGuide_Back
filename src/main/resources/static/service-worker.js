importScripts('https://storage.googleapis.com/workbox-cdn/releases/7.0.0/workbox-sw.js');

// ===================== Workbox 설정 =====================
workbox.setConfig({ debug: false });

// ===================== 정적 자산: Cache First =====================
workbox.routing.registerRoute(
  ({ request }) => request.destination === 'image',
  new workbox.strategies.CacheFirst({
    cacheName: 'images-cache',
    plugins: [
      new workbox.expiration.ExpirationPlugin({
        maxEntries: 100,
        maxAgeSeconds: 30 * 24 * 60 * 60 // 30일
      }),
      new workbox.cacheableResponse.CacheableResponsePlugin({
        statuses: [0, 200]
      })
    ]
  })
);

// ===================== Google Fonts: Stale While Revalidate =====================
workbox.routing.registerRoute(
  ({ url }) => url.origin === 'https://fonts.googleapis.com',
  new workbox.strategies.StaleWhileRevalidate({
    cacheName: 'google-fonts-stylesheets'
  })
);

workbox.routing.registerRoute(
  ({ url }) => url.origin === 'https://fonts.gstatic.com',
  new workbox.strategies.CacheFirst({
    cacheName: 'google-fonts-webfonts',
    plugins: [
      new workbox.cacheableResponse.CacheableResponsePlugin({
        statuses: [0, 200]
      }),
      new workbox.expiration.ExpirationPlugin({
        maxAgeSeconds: 60 * 60 * 24 * 365 // 1년
      })
    ]
  })
);

// ===================== API (가이드, 위치): Network First + 오프라인 폴백 =====================
workbox.routing.registerRoute(
  ({ url }) => url.pathname.startsWith('/guide/api/nearby'),
  new workbox.strategies.NetworkFirst({
    cacheName: 'api-cache',
    plugins: [
      new workbox.expiration.ExpirationPlugin({
        maxEntries: 50,
        maxAgeSeconds: 5 * 60 // 5분
      })
    ]
  })
);

// ===================== 정적 HTML/JS/CSS: Stale While Revalidate =====================
workbox.routing.registerRoute(
  ({ request }) =>
    request.destination === 'script' ||
    request.destination === 'style',
  new workbox.strategies.StaleWhileRevalidate({
    cacheName: 'static-resources'
  })
);

// ===================== 오프라인 폴백 =====================
workbox.routing.setCatchHandler(async ({ event }) => {
  if (event.request.destination === 'document') {
    return caches.match('/offline.html');
  }
  return Response.error();
});

// ===================== 푸시 알림 핸들러 =====================
self.addEventListener('push', (event) => {
  if (!event.data) return;
  const data = event.data.json();
  event.waitUntil(
    self.registration.showNotification(data.title || 'SnapGuide', {
      body: data.body || '',
      icon: '/icons/icon-192.png',
      badge: '/icons/icon-192.png',
      data: { url: data.url || '/' }
    })
  );
});

// ===================== 알림 클릭 → 앱 열기 =====================
self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      const targetUrl = event.notification.data.url || '/';
      for (const client of clientList) {
        if (client.url === targetUrl && 'focus' in client) {
          return client.focus();
        }
      }
      if (clients.openWindow) {
        return clients.openWindow(targetUrl);
      }
    })
  );
});
