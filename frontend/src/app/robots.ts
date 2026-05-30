import type { MetadataRoute } from 'next'

const BASE_URL = 'https://snapguide.mungji.com'

export default function robots(): MetadataRoute.Robots {
  return {
    rules: [
      {
        userAgent: '*',
        allow: ['/', '/feed', '/guides'],
        disallow: ['/profile/', '/upload/', '/admin/'],
      },
    ],
    sitemap: `${BASE_URL}/sitemap.xml`,
  }
}
