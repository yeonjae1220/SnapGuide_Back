'use client'

export const dynamic = 'force-dynamic'

import { useCallback, useEffect, useRef, useState } from 'react'
import Script from 'next/script'
import { api } from '@/lib/api'
import { useAuthStore } from '@/stores/useAuthStore'
import { useI18n } from '@/i18n/I18nProvider'
import { GuideCard } from '@/components/GuideCard'
import { GuideDetailModal } from '@/components/GuideDetailModal'
import type { Guide } from '@/lib/types'

const DEFAULT_LAT = 37.5665
const DEFAULT_LNG = 126.978
const DEFAULT_RADIUS = 3

export default function FeedPage() {
  const { t } = useI18n()
  const accessToken = useAuthStore((s) => s.accessToken)

  const mapRef = useRef<HTMLDivElement>(null)
  const googleMapRef = useRef<google.maps.Map | null>(null)
  const [mapsKey, setMapsKey] = useState<string | null>(null)
  const [mapsReady, setMapsReady] = useState(false)

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

  // 마운트 시 현재 위치 요청 → 성공하면 그 좌표로, 실패하면 서울 기본값으로 가이드 로드
  useEffect(() => {
    if (!navigator.geolocation) {
      fetchGuides(DEFAULT_LAT, DEFAULT_LNG, DEFAULT_RADIUS)
      return
    }
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        const la = coords.latitude
        const ln = coords.longitude
        latRef.current = la
        lngRef.current = ln
        setLat(la)
        setLng(ln)
        googleMapRef.current?.panTo({ lat: la, lng: ln })
        fetchGuides(la, ln, DEFAULT_RADIUS)
      },
      () => {
        // 위치 권한 거부 또는 실패 → 서울 기본값
        fetchGuides(DEFAULT_LAT, DEFAULT_LNG, DEFAULT_RADIUS)
      },
      { timeout: 8000 },
    )
  }, [fetchGuides])

  const initMap = useCallback(() => {
    if (!mapRef.current || !window.google) return
    const map = new window.google.maps.Map(mapRef.current, {
      center: { lat: latRef.current, lng: lngRef.current },
      zoom: 13,
      disableDefaultUI: true,
      zoomControl: true,
    })
    googleMapRef.current = map
  }, [])

  useEffect(() => {
    if (mapsReady) initMap()
  }, [mapsReady, initMap])

  const handleMyLocation = () => {
    navigator.geolocation?.getCurrentPosition(
      ({ coords }) => {
        const la = coords.latitude
        const ln = coords.longitude
        latRef.current = la
        lngRef.current = ln
        setLat(la)
        setLng(ln)
        googleMapRef.current?.panTo({ lat: la, lng: ln })
        fetchGuides(la, ln, radius)
      },
      undefined,
      { timeout: 8000 },
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
          src={`https://maps.googleapis.com/maps/api/js?key=${mapsKey}&libraries=places`}
          onLoad={() => setMapsReady(true)}
        />
      )}

      <div className="relative">
        {/* map */}
        <div ref={mapRef} className="h-[45vh] w-full bg-gray-100" />

        {/* controls */}
        <div className="absolute left-3 right-3 top-3 flex gap-2">
          <div className="relative flex-1">
            <input
              value={searchInput}
              onChange={(e) => handleSearch(e.target.value)}
              placeholder={t('explore.searchPlaceholder')}
              className="w-full rounded-xl bg-white px-4 py-2 text-sm shadow outline-none"
            />
            {predictions.length > 0 && (
              <ul className="absolute left-0 right-0 top-full z-10 mt-1 rounded-xl bg-white shadow-lg">
                {predictions.map((p) => (
                  <li
                    key={p.place_id}
                    onClick={() => selectPrediction(p.place_id)}
                    className="cursor-pointer px-4 py-2 text-sm hover:bg-gray-50"
                  >
                    {p.description}
                  </li>
                ))}
              </ul>
            )}
          </div>
          <button
            onClick={handleMyLocation}
            className="rounded-xl bg-white px-3 py-2 text-sm shadow"
          >
            📍
          </button>
        </div>

        {/* radius */}
        <div className="absolute bottom-3 left-3 flex items-center gap-2 rounded-xl bg-white px-3 py-1.5 shadow text-xs">
          <span className="text-gray-500">{t('explore.radiusLabel')}</span>
          {[1, 3, 5, 10].map((r) => (
            <button
              key={r}
              onClick={() => handleRadiusChange(r)}
              className={`rounded-lg px-2 py-1 font-medium transition ${
                radius === r
                  ? 'bg-pink-500 text-white'
                  : 'text-gray-600 hover:bg-gray-100'
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
          <p className="py-12 text-center text-sm text-gray-400">{t('guide.empty')}</p>
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
