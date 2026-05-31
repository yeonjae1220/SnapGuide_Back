# Auth Token Security Review

> Next.js 전환 이후 SnapGuide refresh token 흐름 재검토 및 ToneBridge 비교 분석
> 작성일: 2026-05-31

---

## SnapGuide — 발견된 이슈

### [P1] Refresh token 미회전 문제

**위치**: `AuthService.java:130`

만료 3일 이상 남은 경우 access token만 재발급하고 기존 refresh token을 유지합니다.

- 이전 구조에서는 access token도 요구했으나, Next.js 전환 후 cookie의 refresh token만으로 재발급이 가능해짐
- **탈취된 refresh token의 재사용 창이 최대 30일로 확대**됨

**권장 수정**: 매 refresh 요청마다 새 refresh token 발급 + 기존 token Redis 삭제 또는 blacklist 처리 (ToneBridge 방식 참고)

---

### [P2] Refresh token 원문 Redis 인덱스 노출

**위치**: `RedisRefreshToken.java:22` (`@Indexed`)

`@Indexed`가 조회 성능은 높이지만, refresh token 원문이 Redis 인덱스 구조 / keyspace / 백업 / 운영 로그에 더 넓게 남을 수 있습니다.

**권장 수정**: refresh token 원문 대신 `HMAC-SHA256(token)` 해시를 별도 필드로 저장하고 그 값으로 조회

---

### [P2] `findAll()` fallback — 전체 세션 스캔 위험

**위치**: `AuthService.java:147`

기존 배포 토큰 호환을 위한 임시 fallback이지만, 유효한 서명을 가진 미등록 token이 반복 요청되면 Redis 전체 세션 스캔으로 이어질 수 있습니다.

**권장 수정**: 마이그레이션 완료 후 제거하거나, `refreshTokenHash → email` 별도 매핑으로 교체

---

### 양호한 부분

| 항목 | 내용 |
|------|------|
| Cookie 설정 | `HttpOnly`, `Secure`, `SameSite=Lax`, `Path=/api/auth` — CSRF 표면 최소화 |
| Google redirect_uri | `window.location.origin` + 백엔드 허용 목록 검증으로 open redirect 위험 제한 |
| Access token 저장 | 메모리 기반 유지 — localStorage 탈취 위험 없음 |

---

## ToneBridge — 비교 분석

SnapGuide 이슈 기준으로 대체로 더 안전한 구조입니다.

### 양호

| 항목 | 위치 | 내용 |
|------|------|------|
| Refresh 흐름 | `AuthService.java:64` | refresh token 쿠키만으로 처리, access token 본문 불필요 |
| Token rotation | `AuthService.java:72` | 매 refresh마다 기존 token 삭제 + 새 access/refresh token 발급 |
| Redis 스캔 없음 | `RefreshTokenRedisAdapter.java:31` | Redis key 직접 조회, `findAll()` fallback 없음 |

### 개선 필요

**위치**: `RefreshTokenRedisAdapter.java:22`

`refresh:{refreshToken}` 형태로 Redis key를 구성하고, 사용자별 set에도 refresh token 원문을 저장합니다. Redis 접근권이 노출되면 token 원문이 그대로 드러납니다.

**권장 수정**: Redis key/set member를 `HMAC-SHA256(refreshToken)` 해시값으로 교체

---

## 우선순위 정리

| 우선순위 | 프로젝트 | 작업 |
|---------|---------|------|
| P1 | SnapGuide | Refresh token rotation 도입 (매 요청마다 교체) |
| P2 | SnapGuide | `@Indexed` 제거 + 해시 기반 조회로 교체 |
| P2 | SnapGuide | `findAll()` fallback 제거 |
| P2 | ToneBridge | Redis key/set을 token 해시로 교체 |
