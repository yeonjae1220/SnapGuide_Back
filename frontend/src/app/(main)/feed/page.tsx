'use client'

export const dynamic = 'force-dynamic'

import { useCallback, useEffect, useRef, useState } from 'react'
import Script from 'next/script'
import axios from 'axios'
import { api } from '@/lib/api'
import { useAuthStore } from '@/stores/useAuthStore'
import { useI18n } from '@/i18n/I18nProvider'
import { GuideCard } from '@/components/GuideCard'
import { GuideDetailModal } from '@/components/GuideDetailModal'
import { useTheme } from '@/theme/ThemeProvider'
import { useGuideMarkers } from '@/hooks/useGuideMarkers'
import { useRegionMarkers, type RegionCluster } from '@/hooks/useRegionMarkers'
import { useDebouncedCallback } from '@/hooks/useDebouncedCallback'
import { fetchIpLocation } from '@/lib/geoip'
import type { Guide } from '@/lib/types'

const DEFAULT_LAT = 37.5665
const DEFAULT_LNG = 126.978
const DEFAULT_RADIUS = 3
const MAX_RADIUS = 100
/** 이 줌 이상에서만 상세 가이드 목록을 표시한다. 미만이면 국가/대륙 집계 모드. */
const DETAIL_ZOOM_THRESHOLD = 9
/** 줌 < 5이면 대륙 단위, 5 ≤ zoom < 9이면 국가 단위 */
const CONTINENT_ZOOM_THRESHOLD = 5
const DEG_TO_KM = 111
const GPS_TIMEOUT_MS = 3000

const DARK_MAP_STYLES: google.maps.MapTypeStyle[] = [
  { elementType: 'geometry', stylers: [{ color: '#1d1f24' }] },
  { elementType: 'labels.text.fill', stylers: [{ color: '#c9c9d2' }] },
  { elementType: 'labels.text.stroke', stylers: [{ color: '#1d1f24' }] },
  { featureType: 'administrative', elementType: 'geometry.stroke', stylers: [{ color: '#3a3b44' }] },
  { featureType: 'landscape.natural', elementType: 'geometry', stylers: [{ color: '#181a1f' }] },
  { featureType: 'poi', elementType: 'geometry', stylers: [{ color: '#24262d' }] },
  { featureType: 'poi.park', elementType: 'geometry', stylers: [{ color: '#17251c' }] },
  { featureType: 'road', elementType: 'geometry', stylers: [{ color: '#30323a' }] },
  { featureType: 'road', elementType: 'geometry.stroke', stylers: [{ color: '#202229' }] },
  { featureType: 'transit', elementType: 'geometry', stylers: [{ color: '#252832' }] },
  { featureType: 'water', elementType: 'geometry', stylers: [{ color: '#111d2a' }] },
]

const round3 = (n: number) => Math.round(n * 1000) / 1000

// 경도를 [-180, 180) 범위로 정규화한다. 지도를 축소한 채 월드 카피를 횡단하면
// getCenter().lng()가 범위를 벗어난 값(예: -220)을 반환해 백엔드 좌표 검증(±180)에
// 걸려 요청이 실패하므로, 백엔드로 보내기 전 반드시 정규화한다.
const normalizeLng = (lng: number) => ((((lng + 180) % 360) + 360) % 360) - 180
const clampLat = (lat: number) => Math.max(-90, Math.min(90, lat))

// 모듈 스코프 상수: 콜백 이름 충돌 방지 및 렌더 간 안정적 참조
const MAPS_CALLBACK_NAME = '__snapguideMapsReady'

type FetchError = 'network' | null

export default function FeedPage() {
  const { t } = useI18n()
  const { theme } = useTheme()
  const accessToken = useAuthStore((s) => s.accessToken)

  const mapRef = useRef<HTMLDivElement>(null)
  const googleMapRef = useRef<google.maps.Map | null>(null)
  const [map, setMap] = useState<google.maps.Map | null>(null)
  const [mapsKey, setMapsKey] = useState<string | null>(null)
  const [mapsReady, setMapsReady] = useState(false)

  const latRef = useRef(DEFAULT_LAT)
  const lngRef = useRef(DEFAULT_LNG)
  const [lat, setLat] = useState(DEFAULT_LAT)
  const [lng, setLng] = useState(DEFAULT_LNG)
  const [radius, setRadius] = useState(DEFAULT_RADIUS)
  const [autoRadius, setAutoRadius] = useState(true)
  const [guides, setGuides] = useState<Guide[]>([])
  const [selected, setSelected] = useState<Guide | null>(null)
  const [highlightedId, setHighlightedId] = useState<number | null>(null)
  const [loading, setLoading] = useState(false)
  const [fetchError, setFetchError] = useState<FetchError>(null)
  const [searchInput, setSearchInput] = useState('')
  const [predictions, setPredictions] = useState<google.maps.places.AutocompletePrediction[]>([])
  const [locating, setLocating] = useState(false)
  const [locateFailed, setLocateFailed] = useState(false)

  // 집계 모드: zoom < DETAIL_ZOOM_THRESHOLD 일 때 가이드 목록 대신 국가/대륙 클러스터 표시
  const [aggregateClusters, setAggregateClusters] = useState<RegionCluster[]>([])
  const [aggregateMode, setAggregateMode] = useState(false)
  // drillIn ref: useRegionMarkers보다 먼저 선언해 forward-reference 문제 방지
  const drillInRef = useRef<(cluster: RegionCluster) => void>(() => {})

  const canHoverRef = useRef(false)
  const prefersReducedRef = useRef(false)
  useEffect(() => {
    canHoverRef.current = window.matchMedia('(hover: hover)').matches
    prefersReducedRef.current = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  }, [])

  const abortRef = useRef<AbortController | null>(null)
  const lastQueryRef = useRef('')

  const fetchGuides = useCallback(
    async (la: number, ln: number, r: number) => {
      const rLat = round3(clampLat(la))
      const rLng = round3(normalizeLng(ln))
      const rRadius = Math.min(r, MAX_RADIUS)
      const key = `${rLat}:${rLng}:${rRadius}`
      if (key === lastQueryRef.current) return
      lastQueryRef.current = key

      abortRef.current?.abort()
      const controller = new AbortController()
      abortRef.current = controller

      setLoading(true)
      setFetchError(null)
      try {
        const { data } = await api.get(
          `/guide/api/nearby?lat=${rLat}&lng=${rLng}&radius=${rRadius}`,
          { signal: controller.signal },
        )
        setGuides(data)
      } catch (err) {
        // 취소는 정상 종료 — 로딩 상태는 새 요청이 담당하므로 여기서 처리하지 않음
        if (axios.isCancel(err)) return
        lastQueryRef.current = ''
        setGuides([])
        setFetchError('network')
      } finally {
        if (abortRef.current === controller) {
          setLoading(false)
          abortRef.current = null
        }
      }
    },
    [],
  )

  const moveMapTo = useCallback((la: number, ln: number) => {
    const m = googleMapRef.current
    if (!m) return
    if (prefersReducedRef.current) m.setCenter({ lat: la, lng: ln })
    else m.panTo({ lat: la, lng: ln })
  }, [])

  useEffect(() => {
    if (!accessToken || mapsKey) return
    api.get('/api/maps/key').then(({ data }) => setMapsKey(data.apiKey))
  }, [accessToken, mapsKey])

  const applyLocation = useCallback(
    (la: number, ln: number) => {
      latRef.current = la
      lngRef.current = ln
      setLat(la)
      setLng(ln)
      moveMapTo(la, ln)
      fetchGuides(la, ln, DEFAULT_RADIUS)
    },
    [fetchGuides, moveMapTo],
  )

  // 기본값(서울)으로 즉시 가이드 로딩 후, IP 기반 대략 위치로 갱신.
  // 마운트 시 GPS(navigator.geolocation)를 호출하지 않으므로 권한 프롬프트나
  // CoreLocation 콘솔 에러가 발생하지 않는다. 정밀 위치는 "현재 위치" 버튼에서 GPS로 처리.
  useEffect(() => {
    fetchGuides(DEFAULT_LAT, DEFAULT_LNG, DEFAULT_RADIUS)
    fetchIpLocation().then((coords) => {
      if (coords) applyLocation(coords.lat, coords.lng)
    })
  // applyLocation은 마운트 후 변경되지 않으므로 의도적으로 deps에서 제외
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const initMap = useCallback(() => {
    if (!mapRef.current || !window.google) return
    if (googleMapRef.current) {
      // 재진입(탭 복귀) 시 이미 초기화된 맵을 재사용하고 리사이즈만 트리거
      google.maps.event.trigger(googleMapRef.current, 'resize')
      return
    }
    const m = new window.google.maps.Map(mapRef.current, {
      center: { lat: latRef.current, lng: lngRef.current },
      zoom: 13,
      disableDefaultUI: true,
      zoomControl: true,
      styles: theme === 'dark' ? DARK_MAP_STYLES : undefined,
    })
    googleMapRef.current = m
    setMap(m)
  }, [theme])

  useEffect(() => {
    // loading=async 콜백 패턴: SDK 로드 완료 시 호출됨
    // 콜백 등록을 Script 삽입 전 useEffect에서 처리하므로 race condition 없음
    ;(window as unknown as Record<string, unknown>)[MAPS_CALLBACK_NAME] = () => setMapsReady(true)

    // SPA 리마운트 시 window.google이 이미 존재하면 콜백이 재발동하지 않으므로 직접 초기화
    if (window.google?.maps) initMap()

    return () => {
      delete (window as unknown as Record<string, unknown>)[MAPS_CALLBACK_NAME]
    }
  // initMap 참조는 theme 변경 시에만 바뀌며 맵 재생성 대신 setOptions로 처리하므로 안전
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (mapsReady) initMap()
  }, [mapsReady, initMap])

  // 페이지 가시성 복귀 시 맵 리사이즈 (탭 전환 후 회색 타일 방지)
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible' && googleMapRef.current) {
        google.maps.event.trigger(googleMapRef.current, 'resize')
      }
    }
    document.addEventListener('visibilitychange', handleVisibilityChange)
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange)
  }, [])

  useEffect(() => {
    googleMapRef.current?.setOptions({
      styles: theme === 'dark' ? DARK_MAP_STYLES : undefined,
    })
  }, [theme])

  // 상세 모드(zoom ≥ 9): 가이드 핀 마커
  useGuideMarkers({
    map: aggregateMode ? null : map,
    guides,
    highlightedId,
    hoverEnabled: canHoverRef.current,
    onClick: setSelected,
    onHover: setHighlightedId,
  })

  // 집계 모드(zoom < 9): 국가/대륙 원형 사진 오버레이 (ref로 콜백 전달 — forward-reference 방지)
  useRegionMarkers({
    map: aggregateMode ? map : null,
    clusters: aggregateClusters,
    onDrillIn: useCallback((c: RegionCluster) => drillInRef.current(c), []),
  })

  useEffect(() => {
    if (highlightedId == null) return
    const el = document.querySelector(`[data-guide-card="${highlightedId}"]`)
    el?.scrollIntoView({ block: 'nearest', behavior: prefersReducedRef.current ? 'auto' : 'smooth' })
  }, [highlightedId])

  const viewportRadiusKm = (m: google.maps.Map): number => {
    const bounds = m.getBounds()
    if (!bounds) return DEFAULT_RADIUS
    const ne = bounds.getNorthEast()
    const sw = bounds.getSouthWest()
    const latKm = Math.abs(ne.lat() - sw.lat()) * DEG_TO_KM
    const lngKm =
      Math.abs(ne.lng() - sw.lng()) * DEG_TO_KM * Math.cos((ne.lat() * Math.PI) / 180)
    return Math.min(Math.ceil(Math.max(latKm, lngKm) / 2), MAX_RADIUS)
  }

  const lastAggLevelRef = useRef<string | null>(null)

  const fetchAggregate = useCallback(async (zoom: number) => {
    const level = zoom < CONTINENT_ZOOM_THRESHOLD ? 'continent' : 'country'
    // level이 같으면 결과도 동일(전역 집계) — idle 때마다 재요청 방지
    if (level === lastAggLevelRef.current) return
    lastAggLevelRef.current = level
    try {
      const { data } = await api.get<RegionCluster[]>(`/guide/api/aggregate?level=${level}`)
      setAggregateClusters(data ?? [])
    } catch {
      setAggregateClusters([])
    }
  }, [])

  const debouncedRefetch = useDebouncedCallback(() => {
    const m = googleMapRef.current
    if (!m) return
    const zoom = m.getZoom() ?? 0

    if (zoom < DETAIL_ZOOM_THRESHOLD) {
      // 집계 모드: 가이드 목록 고정, 국가/대륙 클러스터만 갱신
      setAggregateMode(true)
      fetchAggregate(zoom)
      return
    }

    // 상세 모드: 기존 동작
    setAggregateMode(false)
    setAggregateClusters([])
    lastQueryRef.current = ''    // 집계 중 건너뛴 가이드 재요청 보장
    lastAggLevelRef.current = null  // 다음 집계 모드 진입 시 재요청 보장
    const center = m.getCenter()
    if (!center) return
    const la = center.lat()
    const ln = center.lng()
    latRef.current = la
    lngRef.current = ln
    setLat(la)
    setLng(ln)
    const r = autoRadius ? viewportRadiusKm(m) : radius
    fetchGuides(la, ln, r)
  }, 400)

  // 클러스터 클릭 → 해당 위치로 이동하며 상세 줌으로 진입
  drillInRef.current = (cluster: RegionCluster) => {
    const m = googleMapRef.current
    if (!m) return
    m.setCenter({ lat: cluster.lat, lng: cluster.lng })
    m.setZoom(DETAIL_ZOOM_THRESHOLD)
  }

  useEffect(() => {
    if (!map) return
    const listener = map.addListener('idle', debouncedRefetch)
    return () => google.maps.event.removeListener(listener)
  }, [map, debouncedRefetch])

  // IP 기반 폴백. 브라우저 위치(CoreLocation 등)가 실패할 때 사용한다.
  // 공개 geo-IP API를 브라우저에서 직접 호출해 사용자 실제 공인 IP를 사용한다
  // (서버사이드는 홈랩 NAT로 클라이언트 IP를 식별하지 못함).
  const locateByIp = useCallback(async (): Promise<boolean> => {
    const coords = await fetchIpLocation()
    if (coords) {
      applyLocation(coords.lat, coords.lng)
      return true
    }
    return false
  }, [applyLocation])

  const handleMyLocation = useCallback(() => {
    setLocateFailed(false)
    setLocating(true)
    const finishWithIp = async () => {
      const ok = await locateByIp()
      setLocating(false)
      if (!ok) setLocateFailed(true)
    }
    if (!navigator.geolocation) {
      finishWithIp()
      return
    }
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        applyLocation(coords.latitude, coords.longitude)
        setLocating(false)
      },
      () => finishWithIp(),
      { timeout: GPS_TIMEOUT_MS, maximumAge: 60000 },
    )
  }, [applyLocation, locateByIp])

  // 위치 실패 안내는 3초 후 자동 해제
  useEffect(() => {
    if (!locateFailed) return
    const id = setTimeout(() => setLocateFailed(false), 3000)
    return () => clearTimeout(id)
  }, [locateFailed])

  const handleRadiusChange = (r: number) => {
    if (aggregateMode) return
    setAutoRadius(false)
    setRadius(r)
    fetchGuides(lat, lng, r)
  }

  const handleAutoRadius = () => {
    if (aggregateMode) return
    setAutoRadius(true)
    const m = googleMapRef.current
    if (m) fetchGuides(lat, lng, viewportRadiusKm(m))
  }

  const handleSearchInput = useDebouncedCallback((input: string) => {
    if (!window.google) return
    const svc = new window.google.maps.places.AutocompleteService()
    svc.getPlacePredictions({ input }, (results) => setPredictions(results ?? []))
  }, 300)

  const handleSearch = (input: string) => {
    setSearchInput(input)
    if (!input) {
      setPredictions([])  // 검색어 비울 때 즉시 닫기 (디바운스 건너뜀)
      return
    }
    handleSearchInput(input)
  }

  const selectPrediction = (placeId: string) => {
    const m = googleMapRef.current
    if (!m) return
    const svc = new window.google.maps.places.PlacesService(m)
    svc.getDetails({ placeId }, (place) => {
      if (!place?.geometry?.location) return
      const la = place.geometry.location.lat()
      const ln = place.geometry.location.lng()
      latRef.current = la
      lngRef.current = ln
      setLat(la)
      setLng(ln)
      setPredictions([])
      setSearchInput(place.name ?? '')
      // 집계 모드(zoom < 9)에서 검색하면 상세 줌으로 진입한다.
      // idle → debouncedRefetch가 fetchGuides를 담당하므로 직접 호출하지 않는다.
      if ((m.getZoom() ?? 0) < DETAIL_ZOOM_THRESHOLD) {
        m.setCenter({ lat: la, lng: ln })
        m.setZoom(DETAIL_ZOOM_THRESHOLD)
      } else {
        moveMapTo(la, ln)
        fetchGuides(la, ln, autoRadius ? DEFAULT_RADIUS : radius)
      }
    })
  }

  const cardHover = (id: number | null) => {
    if (canHoverRef.current) setHighlightedId(id)
  }

  return (
    <>
      {mapsKey && (
        <Script
          nonce={document.body.dataset.nonce}
          src={`https://maps.googleapis.com/maps/api/js?key=${mapsKey}&libraries=places&loading=async&callback=${MAPS_CALLBACK_NAME}`}
          strategy="afterInteractive"
        />
      )}

      <div className="relative">
        {/* map */}
        <div ref={mapRef} className="h-[45vh] w-full bg-surface-muted transition-colors duration-200" />

        {/* loading spinner */}
        {loading && (
          <div
            role="status"
            aria-label={t('common.loading')}
            aria-live="polite"
            className="pointer-events-none absolute right-3 top-3 flex h-9 w-9 items-center justify-center rounded-full border border-line bg-surface/95 shadow-card backdrop-blur"
          >
            <span aria-hidden="true" className="h-4 w-4 animate-spin rounded-full border-2 border-accent border-t-transparent" />
          </div>
        )}

        {/* controls */}
        <div className="absolute left-3 right-3 top-3 flex gap-2">
          <div className="relative flex-1" role="combobox" aria-expanded={predictions.length > 0} aria-haspopup="listbox" aria-owns="place-predictions">
            <input
              value={searchInput}
              onChange={(e) => handleSearch(e.target.value)}
              placeholder={t('explore.searchPlaceholder')}
              aria-label={t('explore.searchPlaceholder')}
              aria-autocomplete="list"
              aria-controls="place-predictions"
              className="w-full rounded-xl border border-line bg-surface/95 px-4 py-2 text-sm text-ink shadow-card outline-none backdrop-blur transition-colors duration-200 placeholder:text-subtle focus:border-accent/60 focus:ring-2 focus:ring-accent/15"
            />
            {predictions.length > 0 && (
              <ul id="place-predictions" role="listbox" aria-label={t('explore.searchPlaceholder')} className="absolute left-0 right-0 top-full z-10 mt-1 overflow-hidden rounded-xl border border-line bg-surface-elevated shadow-card">
                {predictions.map((p) => (
                  <li
                    key={p.place_id}
                    role="option"
                    aria-selected={false}
                    onClick={() => selectPrediction(p.place_id)}
                    className="cursor-pointer px-4 py-2 text-sm text-ink transition-colors hover:bg-surface-muted"
                  >
                    {p.description}
                  </li>
                ))}
              </ul>
            )}
          </div>
          <button
            onClick={handleMyLocation}
            disabled={locating}
            aria-label={t('explore.myLocation')}
            aria-busy={locating}
            className="rounded-xl border border-line bg-surface/95 px-3 py-2 text-sm shadow-card backdrop-blur transition-colors hover:bg-surface-elevated disabled:opacity-60"
          >
            {locating ? (
              <span aria-hidden="true" className="block h-4 w-4 animate-spin rounded-full border-2 border-accent border-t-transparent" />
            ) : (
              '📍'
            )}
          </button>
        </div>

        {/* 위치 확인 실패 안내 */}
        {locateFailed && (
          <div
            role="status"
            aria-live="polite"
            className="absolute left-1/2 top-16 z-10 -translate-x-1/2 rounded-lg border border-line bg-surface/95 px-3 py-1.5 text-xs text-muted shadow-card backdrop-blur"
          >
            {t('explore.locateFailed')}
          </div>
        )}

        {/* radius — 집계 모드(zoom < 9)에서는 무의미하므로 숨김 */}
        <div className={`absolute bottom-3 left-3 flex items-center gap-2 rounded-xl border border-line bg-surface/95 px-3 py-1.5 text-xs shadow-card backdrop-blur transition-colors duration-200 ${aggregateMode ? 'hidden' : ''}`}>
          <span className="text-muted">{t('explore.radiusLabel')}</span>
          <button
            onClick={handleAutoRadius}
            className={`rounded-lg px-2 py-1 font-medium transition ${
              autoRadius
                ? 'bg-accent text-white'
                : 'text-muted hover:bg-surface-muted hover:text-ink'
            }`}
          >
            Auto
          </button>
          {[1, 3, 5, 10].map((r) => (
            <button
              key={r}
              onClick={() => handleRadiusChange(r)}
              className={`rounded-lg px-2 py-1 font-medium transition ${
                !autoRadius && radius === r
                  ? 'bg-accent text-white'
                  : 'text-muted hover:bg-surface-muted hover:text-ink'
              }`}
            >
              {r}km
            </button>
          ))}
        </div>
      </div>

      {/* guide list / aggregate hint */}
      <div className="p-4">
        {aggregateMode ? (
          <p className="py-12 text-center text-sm text-subtle">
            {t('explore.zoomInHint')}
          </p>
        ) : fetchError === 'network' ? (
          <p className="py-12 text-center text-sm text-danger">{t('common.error')}</p>
        ) : guides.length === 0 && !loading ? (
          <p className="py-12 text-center text-sm text-subtle">{t('guide.empty')}</p>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2">
            {guides.map((g) => (
              <GuideCard
                key={g.id}
                guide={g}
                onOpen={setSelected}
                onHover={cardHover}
                highlighted={highlightedId === g.id}
              />
            ))}
          </div>
        )}
      </div>

      {selected && <GuideDetailModal guide={selected} onClose={() => setSelected(null)} />}
    </>
  )
}
