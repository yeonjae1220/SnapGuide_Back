# SnapGuide Frontend — 아키텍처

> Next.js 15 App Router, TypeScript, Tailwind CSS

---

## 디렉토리 구조

```
frontend/src/
├── app/
│   ├── layout.tsx          # 루트 레이아웃 (nonce 읽기)
│   ├── middleware.ts        # CSP nonce + Google Maps 도메인 허용
│   ├── page.tsx            # 랜딩 (OAuth code 처리 포함)
│   └── (main)/
│       ├── layout.tsx      # InitAuth + 네비게이션
│       ├── feed/           # 지도 + 주변 가이드 목록
│       ├── guides/         # 내 가이드 목록
│       ├── upload/         # 가이드 업로드
│       └── profile/        # 프로필
├── components/
│   ├── InitAuth.tsx        # 토큰 초기화 (OAuth code 처리 + reissue)
│   ├── GuideCard.tsx       # 가이드 카드
│   ├── GuideDetailModal.tsx
│   ├── Navbar.tsx
│   └── landing/            # 랜딩 히어로 + 로그인 패널
├── stores/
│   └── useAuthStore.ts     # Zustand (persist 없음 — 메모리 전용)
├── lib/
│   ├── api.ts              # axios 클라이언트 (401 → reissue)
│   ├── types.ts            # Guide, Media, Author 타입
│   ├── jwt.ts              # JWT payload 파싱
│   ├── geoip.ts            # 클라이언트사이드 geo-IP (ipapi.co → ipwho.is 순차 폴백)
│   └── mapPins.ts          # 가이드 핀 아이콘 빌더
├── hooks/
│   ├── useGuideMarkers.ts  # 상세 모드 마커 + MarkerClusterer
│   ├── useRegionMarkers.ts # 집계 모드 국가/대륙 OverlayView 원형 포토
│   └── useDebouncedCallback.ts
└── i18n/                   # 다국어
```

---

## Google Maps 연동

`Feed` 페이지에서 Maps JS SDK를 동적 로드합니다:

```
GET /api/maps/key → Maps API key 반환 (서버에서 관리)
→ <Script src="https://maps.googleapis.com/...?key={key}">
→ new google.maps.Map(ref) → fetchGuides(lat, lng, radius)
```

Maps API key는 클라이언트에 직접 노출되지 않고 백엔드 엔드포인트를 통해 전달됩니다.

---

## 줌 단계별 지도 마커 동작

```
zoom < 5            zoom 5 ~ 8          zoom ≥ 9
─────────────       ─────────────       ─────────────
대륙 집계 모드      국가 집계 모드       상세 모드
OverlayView 원형    OverlayView 원형    google.maps.Marker
(6~7개)             (수십 개)           + MarkerClusterer
/aggregate?level=   /aggregate?level=   /nearby?lat&lng&radius
  continent           country
가이드 목록: 고정   가이드 목록: 고정   가이드 목록: 실시간
```

**전환 흐름:**

```
map idle (400ms debounce)
  └─ zoom < 9 → aggregateMode=true, fetchAggregate(zoom)
  └─ zoom ≥ 9 → aggregateMode=false, fetchGuides(center, radius)

집계 모드 → 상세 모드 전환 시:
  lastQueryRef = ''        (집계 중 건너뛴 가이드 재요청 보장)
  lastAggLevelRef = null   (다음 집계 진입 시 재요청 보장)
```

**클러스터 클릭 → drillIn:**
- `m.setCenter(cluster.lat/lng)` + `m.setZoom(9)` → idle 발생 → 상세 모드 자동 전환

**검색(Autocomplete) → selectPrediction:**
- zoom < 9이면 `setZoom(9)` 후 idle에게 fetchGuides 위임 (집계 모드 우회 방지)
- zoom ≥ 9이면 기존처럼 직접 `fetchGuides` 호출

**최적화:**
- `useGuideMarkers`: `map=null` 전달 시 기존 마커 cleanup, 신규 생성 안 함
- `useRegionMarkers`: `map=null` 전달 시 오버레이 전부 `setMap(null)` 정리
- `fetchAggregate`: `lastAggLevelRef`로 level 변경 시만 API 호출 (동일 level 재요청 방지)
- 경도 정규화: `normalizeLng = ((((lng + 180) % 360) + 360) % 360) - 180` (Google Maps 세계 복사본 패닝 대응)

---

## 위치 조회 전략

```
마운트 시:
  1. 서울 기본값으로 즉시 가이드 로딩
  2. fetchIpLocation() (비동기) → 위치 갱신
     └─ ipapi.co → 실패 시 ipwho.is 순차 폴백
     └─ 4s 타임아웃, Null Island(0,0)·범위 검증

"현재 위치" 버튼 클릭:
  1. navigator.geolocation.getCurrentPosition (3s timeout)
  2. GPS 실패 → fetchIpLocation() 폴백
  3. 모두 실패 → "현재 위치를 찾을 수 없습니다" 3초 표시 후 자동 해제
```

**서버사이드 `/api/maps/location`을 사용하지 않는 이유:**  
k8s `NodePort` + nginx-ingress 환경에서 클라이언트 IP가 노드 사설 IP로 SNAT돼  
`getRemoteAddr()` → `isLocalAddress()` 분기 → ipapi 미호출 → 204 반환.  
브라우저 직접 호출은 실제 공인 IP가 사용되므로 이 문제를 우회함.

---

## 인증 흐름

```
[Google OAuth]
  AuthPanel → Google 로그인 클릭
  → saveOauthState(nonce) → 쿠키 저장
  → Google 인가 서버 → ?code=xxx&state=yyy
  → 랜딩 페이지 (/?code=xxx)
  → InitAuth: code 감지 → POST /api/auth/oauth/token
  → { accessToken } → setTokens() → router.replace('/feed')

[페이지 로드 (이미 로그인)]
  InitAuth → accessToken 없음 → POST /api/auth/reissue (쿠키)
  → setTokens(newToken)
```

---

## k8s 환경변수

| 변수 | 값 |
|------|-----|
| `API_URL` | `http://backend.snapguide.svc.cluster.local:8080` |
| `PORT` | `3000` |
| `HOSTNAME` | `0.0.0.0` |
