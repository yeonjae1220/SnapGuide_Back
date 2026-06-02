export type GeoCoords = { lat: number; lng: number }

const GEOIP_TIMEOUT_MS = 4000

/**
 * 공개 geo-IP 제공자 목록. 브라우저에서 직접 호출하므로 사용자의 실제 공인 IP가
 * 사용되어 홈랩/리버스 프록시의 NAT 문제를 우회한다. 모두 HTTPS·CORS·무인증.
 * CSP `connect-src`에 각 origin이 등록되어 있어야 한다 (middleware.ts).
 */
const PROVIDERS: ReadonlyArray<{ url: string; pick: (d: unknown) => [unknown, unknown] }> = [
  {
    url: 'https://ipapi.co/json/',
    pick: (d) => [(d as Record<string, unknown>).latitude, (d as Record<string, unknown>).longitude],
  },
  {
    url: 'https://ipwho.is/',
    pick: (d) => [(d as Record<string, unknown>).latitude, (d as Record<string, unknown>).longitude],
  },
]

function toCoords(raw: [unknown, unknown]): GeoCoords | null {
  const lat = Number(raw[0])
  const lng = Number(raw[1])
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return null
  // 0,0(Null Island)은 실패로 간주 — 제공자가 위치 미상일 때 흔히 반환
  if (lat === 0 && lng === 0) return null
  if (lat < -90 || lat > 90 || lng < -180 || lng > 180) return null
  return { lat, lng }
}

/**
 * 클라이언트 IP 기반 대략적 위치를 조회한다. 제공자를 순서대로 시도하고
 * 첫 성공을 반환하며, 모두 실패하면 null을 반환한다. 예외는 삼키되 throw하지 않는다.
 */
export async function fetchIpLocation(): Promise<GeoCoords | null> {
  for (const provider of PROVIDERS) {
    const coords = await tryProvider(provider.url, provider.pick)
    if (coords) return coords
  }
  return null
}

async function tryProvider(
  url: string,
  pick: (d: unknown) => [unknown, unknown],
): Promise<GeoCoords | null> {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), GEOIP_TIMEOUT_MS)
  try {
    const res = await fetch(url, { signal: controller.signal })
    if (!res.ok) return null
    const data: unknown = await res.json()
    return toCoords(pick(data))
  } catch {
    return null
  } finally {
    clearTimeout(timer)
  }
}
