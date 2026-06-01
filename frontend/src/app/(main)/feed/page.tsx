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
import { useDebouncedCallback } from '@/hooks/useDebouncedCallback'
import type { Guide } from '@/lib/types'

const DEFAULT_LAT = 37.5665
const DEFAULT_LNG = 126.978
const DEFAULT_RADIUS = 3
const MAX_RADIUS = 100
// 줌이 이보다 낮으면(너무 멀리 보면) 자동 재조회를 건너뛴다.
const MIN_AUTO_REFETCH_ZOOM = 9
const DEG_TO_KM = 111

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

// 캐시 적중률을 높이려고 좌표를 소수 3자리로 반올림한다.
const round3 = (n: number) => Math.round(n * 1000) / 1000

export default function FeedPage() {
  const { t } = useI18n()
  const { theme } = useTheme()
  const accessToken = useAuthStore((s) => s.accessToken)

  const mapRef = useRef<HTMLDivElement>(null)
  const googleMapRef = useRef<google.maps.Map | null>(null)
  const [map, setMap] = useState<google.maps.Map | null>(null)
  const [mapsKey, setMapsKey] = useState<string | null>(null)
  const [mapsReady, setMapsReady] = useState(false)
  const callbackName = '__snapguideMapsReady'

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
  const [searchInput, setSearchInput] = useState('')
  const [predictions, setPredictions] = useState<google.maps.places.AutocompletePrediction[]>([])

  // 터치 기기에서는 hover가 없으므로 카드 hover 연동을 비활성화한다.
  const canHoverRef = useRef(false)
  const prefersReducedRef = useRef(false)
  useEffect(() => {
    canHoverRef.current = window.matchMedia('(hover: hover)').matches
    prefersReducedRef.current = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  }, [])

  // 경쟁 상태 방지: 새 요청 시 직전 요청 취소
  const abortRef = useRef<AbortController | null>(null)
  const lastQueryRef = useRef('')

  const fetchGuides = useCallback(
    async (la: number, ln: number, r: number) => {
      const rLat = round3(la)
      const rLng = round3(ln)
      const rRadius = Math.min(r, MAX_RADIUS)
      const key = `${rLat}:${rLng}:${rRadius}`
      if (key === lastQueryRef.current) return
      lastQueryRef.current = key

      abortRef.current?.abort()
      const controller = new AbortController()
      abortRef.current = controller

      setLoading(true)
      try {
        const { data } = await api.get(
          `/guide/api/nearby?lat=${rLat}&lng=${rLng}&radius=${rRadius}`,
          { signal: controller.signal },
        )
        setGuides(data)
      } catch (err) {
        if (axios.isCancel(err)) return
        // 실제 실패 시 좌표 키를 초기화해 재진입 시 재조회를 허용한다.
        lastQueryRef.current = ''
        setGuides([])
      } finally {
        if (abortRef.current === controller) {
          setLoading(false)
          abortRef.current = null
        }
      }
    },
    [],
  )

  // reduced-motion이면 부드러운 panTo 대신 즉시 이동한다.
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

  // 마운트 시 현재 위치 요청 → GPS 실패 시 IP 기반 → 모두 실패 시 서울 기본값
  useEffect(() => {
    const fallbackToIp = () =>
      fetch('https://ipapi.co/json/')
        .then((r) => r.json())
        .then((d) => {
          if (d.latitude && d.longitude) applyLocation(d.latitude, d.longitude)
          else fetchGuides(DEFAULT_LAT, DEFAULT_LNG, DEFAULT_RADIUS)
        })
        .catch(() => fetchGuides(DEFAULT_LAT, DEFAULT_LNG, DEFAULT_RADIUS))

    if (!navigator.geolocation) {
      fallbackToIp()
      return
    }
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => applyLocation(coords.latitude, coords.longitude),
      () => fallbackToIp(),
      { timeout: 5000, maximumAge: 60000 },
    )
  }, [applyLocation, fetchGuides])

  const initMap = useCallback(() => {
    if (!mapRef.current || !window.google) return
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
    // loading=async 경고 제거: onLoad 대신 callback 패턴 사용
    ;(window as unknown as Record<string, unknown>)[callbackName] = () => setMapsReady(true)
    return () => {
      delete (window as unknown as Record<string, unknown>)[callbackName]
    }
  }, [callbackName])

  useEffect(() => {
    if (mapsReady) initMap()
  }, [mapsReady, initMap])

  useEffect(() => {
    googleMapRef.current?.setOptions({
      styles: theme === 'dark' ? DARK_MAP_STYLES : undefined,
    })
  }, [theme])

  // 마커 렌더링 + 클러스터링 + hover/click 연동
  useGuideMarkers({
    map,
    guides,
    highlightedId,
    hoverEnabled: canHoverRef.current,
    onClick: setSelected,
    onHover: setHighlightedId,
  })

  // 마커 hover로 강조된 카드를 목록에서 보이도록 스크롤
  useEffect(() => {
    if (highlightedId == null) return
    const el = document.querySelector(`[data-guide-card="${highlightedId}"]`)
    el?.scrollIntoView({ block: 'nearest', behavior: prefersReducedRef.current ? 'auto' : 'smooth' })
  }, [highlightedId])

  // 뷰포트 대각선으로부터 자동 반경(km) 계산
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

  // 줌/이동이 끝난(idle) 뒤 디바운스 재조회
  const debouncedRefetch = useDebouncedCallback(() => {
    const m = googleMapRef.current
    if (!m) return
    const zoom = m.getZoom() ?? 0
    if (zoom < MIN_AUTO_REFETCH_ZOOM) return // 너무 멀리 보면 skip
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

  useEffect(() => {
    if (!map) return
    const listener = map.addListener('idle', debouncedRefetch)
    return () => google.maps.event.removeListener(listener)
  }, [map, debouncedRefetch])

  const handleMyLocation = () => {
    const applyAndFetch = (la: number, ln: number) => applyLocation(la, ln)
    navigator.geolocation?.getCurrentPosition(
      ({ coords }) => applyAndFetch(coords.latitude, coords.longitude),
      () =>
        fetch('https://ipapi.co/json/')
          .then((r) => r.json())
          .then((d) => { if (d.latitude) applyAndFetch(d.latitude, d.longitude) })
          .catch(() => {}),
      { timeout: 5000 },
    )
  }

  const handleRadiusChange = (r: number) => {
    setAutoRadius(false)
    setRadius(r)
    fetchGuides(lat, lng, r)
  }

  const handleAutoRadius = () => {
    setAutoRadius(true)
    const m = googleMapRef.current
    if (m) fetchGuides(lat, lng, viewportRadiusKm(m))
  }

  const handleSearch = (input: string) => {
    setSearchInput(input)
    if (!input || !window.google) return setPredictions([])
    const svc = new window.google.maps.places.AutocompleteService()
    svc.getPlacePredictions({ input }, (results) => setPredictions(results ?? []))
  }

  const selectPrediction = (placeId: string) => {
    if (!googleMapRef.current) return
    const svc = new window.google.maps.places.PlacesService(googleMapRef.current)
    svc.getDetails({ placeId }, (place) => {
      if (!place?.geometry?.location) return
      const la = place.geometry.location.lat()
      const ln = place.geometry.location.lng()
      latRef.current = la
      lngRef.current = ln
      setLat(la)
      setLng(ln)
      moveMapTo(la, ln)
      fetchGuides(la, ln, autoRadius ? DEFAULT_RADIUS : radius)
      setPredictions([])
      setSearchInput(place.name ?? '')
    })
  }

  const cardHover = (id: number | null) => {
    if (canHoverRef.current) setHighlightedId(id)
  }

  return (
    <>
      {mapsKey && (
        <Script
          src={`https://maps.googleapis.com/maps/api/js?key=${mapsKey}&libraries=places&loading=async&callback=${callbackName}`}
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
            className="pointer-events-none absolute right-3 top-3 flex h-9 w-9 items-center justify-center rounded-full border border-line bg-surface/95 shadow-card backdrop-blur"
          >
            <span aria-hidden="true" className="h-4 w-4 animate-spin rounded-full border-2 border-accent border-t-transparent" />
          </div>
        )}

        {/* controls */}
        <div className="absolute left-3 right-3 top-3 flex gap-2">
          <div className="relative flex-1">
            <input
              value={searchInput}
              onChange={(e) => handleSearch(e.target.value)}
              placeholder={t('explore.searchPlaceholder')}
              className="w-full rounded-xl border border-line bg-surface/95 px-4 py-2 text-sm text-ink shadow-card outline-none backdrop-blur transition-colors duration-200 placeholder:text-subtle focus:border-accent/60 focus:ring-2 focus:ring-accent/15"
            />
            {predictions.length > 0 && (
              <ul className="absolute left-0 right-0 top-full z-10 mt-1 overflow-hidden rounded-xl border border-line bg-surface-elevated shadow-card">
                {predictions.map((p) => (
                  <li
                    key={p.place_id}
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
            className="rounded-xl border border-line bg-surface/95 px-3 py-2 text-sm shadow-card backdrop-blur transition-colors hover:bg-surface-elevated"
          >
            📍
          </button>
        </div>

        {/* radius */}
        <div className="absolute bottom-3 left-3 flex items-center gap-2 rounded-xl border border-line bg-surface/95 px-3 py-1.5 text-xs shadow-card backdrop-blur transition-colors duration-200">
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

      {/* guide list */}
      <div className="p-4">
        {guides.length === 0 ? (
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
