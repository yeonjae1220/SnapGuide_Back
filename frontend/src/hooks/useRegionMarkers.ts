'use client'

import { useEffect, useRef } from 'react'

export type RegionCluster = {
  key: string
  label: string
  lat: number
  lng: number
  count: number
  thumbnailUrl: string | null
}

type Params = {
  map: google.maps.Map | null
  clusters: RegionCluster[]
  onDrillIn: (cluster: RegionCluster) => void
}

const CIRCLE_SIZE = 56  // px — outer circle
const THUMB_SIZE  = 44  // px — photo crop
const BADGE_SIZE  = 20  // px — count badge

// CSS 토큰 참조: 테마 전환 시 일관성 유지
const ACCENT_COLOR = 'var(--color-accent, #6366f1)'

function buildOverlayElement(cluster: RegionCluster): HTMLDivElement {
  const wrap = document.createElement('div')
  wrap.style.cssText = `
    position: absolute;
    width: ${CIRCLE_SIZE}px;
    height: ${CIRCLE_SIZE}px;
    transform: translate(-50%, -50%);
    cursor: pointer;
    user-select: none;
  `

  // Photo circle
  const circle = document.createElement('div')
  circle.style.cssText = `
    width: ${THUMB_SIZE}px;
    height: ${THUMB_SIZE}px;
    border-radius: 50%;
    overflow: hidden;
    border: 2px solid white;
    box-shadow: 0 2px 6px rgba(0,0,0,.35);
    background: ${ACCENT_COLOR};
    display: flex; align-items: center; justify-content: center;
    font-size: 20px;
    margin: ${(CIRCLE_SIZE - THUMB_SIZE) / 2}px auto 0;
  `

  if (cluster.thumbnailUrl) {
    const img = document.createElement('img')
    img.src       = cluster.thumbnailUrl
    img.alt       = cluster.label
    img.style.cssText = `width:100%;height:100%;object-fit:cover;`
    circle.appendChild(img)
  } else {
    // flag indicator fallback — derive regional indicator symbols from 2-letter country code
    const flag = cluster.key.length === 2
      ? String.fromCodePoint(
          ...[...cluster.key.toUpperCase()].map(c => 0x1F1E0 + c.charCodeAt(0) - 65)
        )
      : cluster.label.slice(0, 2).toUpperCase()
    circle.textContent = flag
  }

  // Count badge
  const badge = document.createElement('div')
  badge.style.cssText = `
    position: absolute;
    top: 0; right: 0;
    width: ${BADGE_SIZE}px; height: ${BADGE_SIZE}px;
    border-radius: 50%;
    background: ${ACCENT_COLOR};
    color: white;
    font-size: 10px;
    font-weight: 700;
    display: flex; align-items: center; justify-content: center;
    box-shadow: 0 1px 3px rgba(0,0,0,.4);
    pointer-events: none;
  `
  badge.textContent = cluster.count >= 1000
    ? `${Math.floor(cluster.count / 1000)}k`
    : String(cluster.count)

  // Label tooltip on hover
  const label = document.createElement('div')
  label.textContent = cluster.label
  label.style.cssText = `
    position: absolute;
    bottom: -20px;
    left: 50%;
    transform: translateX(-50%);
    background: rgba(0,0,0,.65);
    color: #fff;
    font-size: 10px;
    white-space: nowrap;
    padding: 1px 5px;
    border-radius: 4px;
    pointer-events: none;
    opacity: 0;
    transition: opacity 150ms;
  `

  wrap.addEventListener('mouseenter', () => { label.style.opacity = '1' })
  wrap.addEventListener('mouseleave', () => { label.style.opacity = '0' })

  wrap.appendChild(circle)
  wrap.appendChild(badge)
  wrap.appendChild(label)

  return wrap
}

/**
 * 줌아웃 상태에서 국가/대륙 단위 사진 원형 오버레이를 지도에 표시한다.
 * OverlayView를 사용해 실제 좌표에 고정된 DOM 요소로 렌더링한다.
 * clusters가 빈 배열이면 오버레이를 모두 제거한다.
 */
export function useRegionMarkers({ map, clusters, onDrillIn }: Params): void {
  const overlaysRef = useRef<google.maps.OverlayView[]>([])
  const onDrillInRef = useRef(onDrillIn)
  onDrillInRef.current = onDrillIn

  useEffect(() => {
    // Remove previous overlays
    overlaysRef.current.forEach(o => o.setMap(null))
    overlaysRef.current = []

    if (!map || clusters.length === 0) return

    clusters.forEach(cluster => {
      const el = buildOverlayElement(cluster)
      el.addEventListener('click', () => onDrillInRef.current(cluster))

      class RegionOverlay extends google.maps.OverlayView {
        private el: HTMLDivElement
        private position: google.maps.LatLng

        constructor(el: HTMLDivElement, lat: number, lng: number) {
          super()
          this.el = el
          this.position = new google.maps.LatLng(lat, lng)
        }

        onAdd(): void {
          this.getPanes()!.overlayMouseTarget.appendChild(this.el)
        }

        draw(): void {
          const proj = this.getProjection()
          const pt   = proj.fromLatLngToDivPixel(this.position)
          if (!pt) return
          this.el.style.left = `${pt.x}px`
          this.el.style.top  = `${pt.y}px`
        }

        onRemove(): void {
          this.el.parentNode?.removeChild(this.el)
        }
      }

      const overlay = new RegionOverlay(el, cluster.lat, cluster.lng)
      overlay.setMap(map)
      overlaysRef.current.push(overlay)
    })

    return () => {
      overlaysRef.current.forEach(o => o.setMap(null))
      overlaysRef.current = []
    }
  }, [map, clusters])
}
