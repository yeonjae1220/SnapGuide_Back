// 숫자 핀 마커용 SVG 아이콘 생성 유틸
// 선택 상태에 따라 색상/크기를 달리해 강조 표시한다.

const PIN_ACCENT = '#6366f1'
const PIN_SELECTED = '#ec4899'
const PIN_BASE_W = 30
const PIN_BASE_H = 40
const PIN_SELECTED_SCALE = 1.15
// 핀 내부 텍스트의 세로 중심을 핀 머리 원에 맞추는 분모값 (경험적으로 2.6이 시각적으로 정렬됨)
const PIN_LABEL_Y_DIVISOR = 2.6

type PinIcon = {
  url: string
  scaledSize: google.maps.Size
  anchor: google.maps.Point
  labelOrigin: google.maps.Point
}

function buildSvg(index: number, selected: boolean): string {
  const fill = selected ? PIN_SELECTED : PIN_ACCENT
  const scale = selected ? PIN_SELECTED_SCALE : 1
  const w = PIN_BASE_W * scale
  const h = PIN_BASE_H * scale
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 30 40">
    <path d="M15 39 C15 39 28 24 28 15 A13 13 0 1 0 2 15 C2 24 15 39 15 39 Z" fill="${fill}" stroke="#ffffff" stroke-width="2"/>
    <circle cx="15" cy="15" r="9" fill="#ffffff" fill-opacity="0.18"/>
    <text x="15" y="20" text-anchor="middle" fill="#ffffff" font-size="13" font-weight="700" font-family="system-ui, sans-serif">${index}</text>
  </svg>`
}

export function buildPinIcon(index: number, selected: boolean): PinIcon {
  const scale = selected ? PIN_SELECTED_SCALE : 1
  const w = PIN_BASE_W * scale
  const h = PIN_BASE_H * scale
  return {
    url: `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(buildSvg(index, selected))}`,
    scaledSize: new google.maps.Size(w, h),
    anchor: new google.maps.Point(w / 2, h),
    labelOrigin: new google.maps.Point(w / 2, h / PIN_LABEL_Y_DIVISOR),
  }
}
