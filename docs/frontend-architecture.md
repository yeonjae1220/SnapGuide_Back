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
│   └── jwt.ts              # JWT payload 파싱
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
