'use client'

export const dynamic = 'force-dynamic'

import { useCallback, useEffect, useRef, useState } from 'react'
import Script from 'next/script'
import { api } from '@/lib/api'
import { useAuthStore } from '@/stores/useAuthStore'
import { useI18n } from '@/i18n/I18nProvider'
import { GuideCard } from '@/components/GuideCard'
import { GuideDetailModal } from '@/components/GuideDetailModal'
import { useTheme } from '@/theme/ThemeProvider'
import type { Guide } from '@/lib/types'

const DEFAULT_LAT = 37.5665
const DEFAULT_LNG = 126.978
const DEFAULT_RADIUS = 3

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

export default function FeedPage() {
  const { t } = useI18n()
  const { theme } = useTheme()
  const accessToken = useAuthStore((s) => s.accessToken)

  const mapRef = useRef<HTMLDivElement>(null)
  const googleMapRef = useRef<google.maps.Map | null>(null)
  const [mapsKey, setMapsKey] = useState<string | null>(null)
  const [mapsReady, setMapsReady] = useState(false)
  const callbackName = '__snapguideMapsReady'

  const latRef = useRef(DEFAULT_LAT)
  const lngRef = useRef(DEFAULT_LNG)
  const [lat, setLat] = useState(DEFAULT_LAT)
  const [lng, setLng] = useState(DEFAULT_LNG)
  const [radius, setRadius] = useState(DEFAULT_RADIUS)
  const [guides, setGuides] = useState<Guide[]>([])
  const [selected, setSelected] = useState<Guide | null>(null)
  const [searchInput, setSearchInput] = useState('')
  const [predictions, setPredictions] = useState<google.maps.places.AutocompletePrediction[]>([])

  const fetchGuides = useCallback(
    async (la: number, ln: number, r: number) => {
      try {
        const headers = accessToken ? { Authorization: `Bearer ${accessToken}` } : {}
        const { data } = await api.get(
          `/guide/api/nearby?lat=${la}&lng=${ln}&radius=${r}`,
          { headers },
        )
        setGuides(data)
      } catch {
        setGuides([])
      }
    },
    [accessToken],
  )

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
      googleMapRef.current?.panTo({ lat: la, lng: ln })
      fetchGuides(la, ln, DEFAULT_RADIUS)
    },
    [fetchGuides],
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
    const map = new window.google.maps.Map(mapRef.current, {
      center: { lat: latRef.current, lng: lngRef.current },
      zoom: 13,
      disableDefaultUI: true,
      zoomControl: true,
      styles: theme === 'dark' ? DARK_MAP_STYLES : undefined,
    })
    googleMapRef.current = map
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

  const handleMyLocation = () => {
    const applyAndFetch = (la: number, ln: number) => {
      latRef.current = la
      lngRef.current = ln
      setLat(la)
      setLng(ln)
      googleMapRef.current?.panTo({ lat: la, lng: ln })
      fetchGuides(la, ln, radius)
    }

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
    setRadius(r)
    fetchGuides(lat, lng, r)
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
      googleMapRef.current?.panTo({ lat: la, lng: ln })
      fetchGuides(la, ln, radius)
      setPredictions([])
      setSearchInput(place.name ?? '')
    })
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
          {[1, 3, 5, 10].map((r) => (
            <button
              key={r}
              onClick={() => handleRadiusChange(r)}
              className={`rounded-lg px-2 py-1 font-medium transition ${
                radius === r
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
              <GuideCard key={g.id} guide={g} onOpen={setSelected} />
            ))}
          </div>
        )}
      </div>

      {selected && <GuideDetailModal guide={selected} onClose={() => setSelected(null)} />}
    </>
  )
}
