# SnapGuide Frontend — 보안 아키텍처

> 마지막 업데이트: 2026-05-31 (CSP unsafe-inline 제거, maps/key 인증 필수화)
> 대상: `frontend/` (Next.js 15, App Router)

---

## 1. 토큰 저장 전략

| 토큰 | 저장 위치 |
|------|-----------|
| `accessToken` | **메모리** (Zustand store, persist 없음) |
| `refreshToken` | **httpOnly 쿠키** (서버 관리) |

`useAuthStore`는 `create()` without `persist` — store가 메모리에만 존재합니다.

**초기화 흐름** (`InitAuth.tsx`):
```
앱 마운트 → URL에 code 파라미터 있으면 → POST /api/auth/oauth/token
          → code 없고 token 없으면 → POST /api/auth/reissue (쿠키 자동 전송)
```

---

## 2. CSP (Content Security Policy)

`src/middleware.ts`에서 요청마다 nonce 생성:

```
script-src 'nonce-{random}' 'strict-dynamic' https://maps.googleapis.com
style-src  'self' 'unsafe-inline' https://fonts.googleapis.com
connect-src 'self' https://maps.googleapis.com https://maps.gstatic.com https://snapguide.mungji.com
worker-src blob:
base-uri 'self'
```

**`'strict-dynamic'`**: nonce로 신뢰된 스크립트가 동적으로 로드하는 하위 스크립트를 허용.  
`'unsafe-inline'`은 `strict-dynamic` 적용 시 현대 브라우저에서 무시되므로 제거함 (2026-05-31).

**Google Maps 허용**: 외부 스크립트라 nonce 직접 적용 불가 → 도메인 allowlist + `strict-dynamic` 조합으로 커버.

**`geolocation=(self)`**: Feed 페이지의 "내 위치" 버튼 (`navigator.geolocation`) 사용.

---

## 3. OAuth state CSRF 방어

```typescript
// AuthPanel.tsx → saveOauthState(state) → SameSite=Lax 쿠키 (5분)
// InitAuth.tsx → consumeOauthState() → 검증 후 쿠키 삭제
```

sessionStorage 대신 쿠키 사용 이유: iOS Safari 인앱 브라우저 호환성.

---

## 4. 백엔드 보안 이슈

`docs/security/auth-token-review.md` 및 `docs/security/code-review-2026-05-31.md` 참조.

### 수정 완료 (2026-05-31)

| 이슈 | 커밋 |
|------|------|
| X-Forwarded-For Rate Limiter 우회 → ForwardedHeaderFilter 위임 | `58a08c1` |
| `/api/maps/key` 미인증 노출 → `.authenticated()` 처리 | `58a08c1` |
| logout 토큰 주체 교차 검증 누락 | `58a08c1` |
| CSP `unsafe-inline` 제거 | `58a08c1` |

### 미수정

| 우선순위 | 이슈 |
|---------|------|
| P1 | Refresh token rotation 미적용 (만료 3일 이상이면 기존 token 유지) |
| P2 | `@Indexed` → refresh token 원문이 Redis 인덱스에 노출 |
| P2 | `findAll()` fallback 제거 필요 |

---

## 5. 보안 헤더

```
X-Frame-Options: DENY
X-Content-Type-Options: nosniff
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: camera=(), microphone=(), geolocation=(self)
```
