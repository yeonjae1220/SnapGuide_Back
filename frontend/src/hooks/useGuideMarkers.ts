import { useEffect, useRef } from 'react'
import { MarkerClusterer } from '@googlemaps/markerclusterer'
import { buildPinIcon } from '@/lib/mapPins'
import type { Guide } from '@/lib/types'

type MarkerEntry = {
  marker: google.maps.Marker
  index: number
}

type Params = {
  map: google.maps.Map | null
  guides: Guide[]
  highlightedId: number | null
  hoverEnabled: boolean
  onClick: (guide: Guide) => void
  onHover: (id: number | null) => void
}

function hasCoords(g: Guide): g is Guide & { latitude: number; longitude: number } {
  return g.latitude != null && g.longitude != null
}

/**
 * 가이드 좌표를 숫자 핀 마커로 렌더링하고 MarkerClusterer로 묶는다.
 * guides 변경 시에만 마커를 재생성하고, 강조(선택/hover)는 아이콘만 교체한다.
 */
export function useGuideMarkers({ map, guides, highlightedId, hoverEnabled, onClick, onHover }: Params): void {
  const clustererRef = useRef<MarkerClusterer | null>(null)
  const entriesRef = useRef<Map<number, MarkerEntry>>(new Map())
  // 콜백 최신 참조 유지 (마커 재생성 방지)
  const onClickRef = useRef(onClick)
  const onHoverRef = useRef(onHover)
  onClickRef.current = onClick
  onHoverRef.current = onHover

  // 마커 생성/정리: map 또는 guides 변경 시
  useEffect(() => {
    if (!map) return

    const entries = new Map<number, MarkerEntry>()
    const markers = guides.filter(hasCoords).map((guide, i) => {
      const index = i + 1
      const marker = new google.maps.Marker({
        position: { lat: guide.latitude, lng: guide.longitude },
        icon: buildPinIcon(index, false),
        title: guide.locationName ?? guide.tip,
      })
      marker.addListener('click', () => onClickRef.current(guide))
      if (hoverEnabled) {
        marker.addListener('mouseover', () => onHoverRef.current(guide.id))
        marker.addListener('mouseout', () => onHoverRef.current(null))
      }
      entries.set(guide.id, { marker, index })
      return marker
    })

    entriesRef.current = entries
    clustererRef.current = new MarkerClusterer({ map, markers })

    return () => {
      clustererRef.current?.clearMarkers()
      clustererRef.current = null
      entries.forEach(({ marker }) => {
        google.maps.event.clearInstanceListeners(marker)
        marker.setMap(null)
      })
      entries.clear()
    }
  }, [map, guides])

  // 강조 변경: 해당 마커 아이콘만 교체 (재생성 없음)
  const prevHighlightRef = useRef<number | null>(null)
  useEffect(() => {
    const entries = entriesRef.current
    const prev = prevHighlightRef.current

    if (prev != null && prev !== highlightedId) {
      const e = entries.get(prev)
      if (e) e.marker.setIcon(buildPinIcon(e.index, false))
    }
    if (highlightedId != null) {
      const e = entries.get(highlightedId)
      if (e) {
        e.marker.setIcon(buildPinIcon(e.index, true))
        e.marker.setZIndex(google.maps.Marker.MAX_ZINDEX + 1)
      }
    }
    prevHighlightRef.current = highlightedId
  }, [highlightedId, guides])
}
