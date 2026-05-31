# 코드 리뷰 결과 및 조치 기록

> 리뷰 일시: 2026-05-31  
> 범위: SnapGuide 전체 코드베이스 (Backend Java/Spring + Frontend Next.js)  
> 커밋: `58a08c1`

---

## HIGH — 4건 (전부 수정 완료)

### [HIGH-1] X-Forwarded-For 헤더 스푸핑으로 Rate Limiter 우회

**원인**  
`AuthController.getClientIp()`가 `X-Forwarded-For` 헤더를 직접 읽어 Rate Limiter 키로 사용했습니다.  
클라이언트가 `X-Forwarded-For: 1.2.3.4` 헤더를 임의로 설정하면 조작된 IP로 카운팅됩니다.

**수정**
- `application.yml`에 `server.forward-headers-strategy: framework` 추가 (docker 프로파일에는 이미 있었음)
- Spring의 `ForwardedHeaderFilter`가 신뢰된 proxy에서 온 헤더만 처리하도록 위임
- `getClientIp()`를 `request.getRemoteAddr()` 단순 반환으로 축소

**파일**
- [`src/main/resources/application.yml`](../application.yml)
- [`AuthController.java:176`](../../src/main/java/yeonjae/snapguide/controller/AuthController.java)

---

### [HIGH-2] `/guide/api/nearby` radius 파라미터 무제한 입력

**원인**  
`@GetMapping("/nearby")`의 `radius` 파라미터에 상한이 없었습니다.  
이 엔드포인트는 JWT 화이트리스트(`GUIDE_PUBLIC_API`)에 포함된 공개 API라서 인증 없이 `radius=99999` 같은 값을 전달하면 DB 전체 스캔에 가까운 쿼리가 발생합니다.

**수정**
- `radius`를 최대 100 km로 클램핑: `Math.min(radius, 100.0)`
- `lat`/`lng` 범위 검증 추가 (`-90~90`, `-180~180`)

**파일**
- [`GuideController.java:121`](../../src/main/java/yeonjae/snapguide/controller/guideController/GuideController.java)

---

### [HIGH-3] Google Maps API 키 미인증 엔드포인트에 노출

**원인**  
`/api/maps/key`가 `SecurityConstants.USER_API` 화이트리스트에 포함되어 있어 누구나 API 키를 조회할 수 있었습니다.  
탈취된 키로 Google Cloud 요금 과금 공격이 가능합니다.

**수정**
- `SecurityConstants.USER_API`에서 `/api/maps/key` 제거
- `SecurityConfig`에 `.requestMatchers("/api/maps/key").authenticated()` 명시적 추가
- 프론트엔드는 인증된 `api` 클라이언트(Authorization 헤더 자동 첨부)로 호출하므로 동작 변경 없음

> **추가 권장**: Google Cloud Console에서 API 키에 HTTP Referrer 제한 (`snapguide.mungji.com`) 설정

**파일**
- [`SecurityConstants.java:25`](../../src/main/java/yeonjae/snapguide/security/constant/SecurityConstants.java)
- [`SecurityConfig.java:206`](../../src/main/java/yeonjae/snapguide/security/config/SecurityConfig.java)

---

### [HIGH-4] logout/delete 엔드포인트의 토큰 주체 교차 검증 누락

**원인**  
`/api/auth/logout`과 `/api/auth/delete`는 요청 바디의 `accessToken`에서 email을 추출해 해당 사용자 세션을 처리합니다.  
JWT 필터는 Authorization 헤더 토큰을 검증하지만, 바디 토큰은 별도 검증 없이 사용합니다.  
유효한 자신의 토큰(Authorization 헤더)과 타인의 만료된 토큰(바디)을 조합하면 타인의 Redis 세션을 삭제할 수 있습니다.

**수정**
- `logout`/`deleteMember` 엔드포인트에 `@AuthenticationPrincipal UserDetails` 파라미터 추가
- `authService.logout(dto)` 반환값(email)과 `userDetails.getUsername()` 비교
- 불일치 시 403 반환

**파일**
- [`AuthController.java:83`](../../src/main/java/yeonjae/snapguide/controller/AuthController.java)

---

## MEDIUM — 5건 (전부 수정 완료)

### [MEDIUM-1] parseClaims 로그 과다 (INFO → DEBUG)

인증 요청마다 토큰 생성일자/만료시각이 `INFO` 레벨로 2개씩 출력됩니다.  
`DEBUG`로 변경하고 2개 로그를 1개로 합쳤습니다.  
`validateToken` 성공 로그도 동일하게 `DEBUG`로 조정.

**파일**: [`JwtTokenProvider.java:157`](../../src/main/java/yeonjae/snapguide/security/authentication/jwt/JwtTokenProvider.java)

---

### [MEDIUM-2] AuthService 불필요한 import 8개 제거

`ResponseEntity`, `@DeleteMapping`, `@Bean`, `@Autowired`, `@AllArgsConstructor`,  
`AuthenticationManagerBuilder`, `HttpSecurity`, `UserDetailsService` — 모두 미사용.  
중복 import(`AuthenticationManager` 두 번)도 함께 정리.

**파일**: [`AuthService.java`](../../src/main/java/yeonjae/snapguide/service/AuthService.java)

---

### [MEDIUM-4] CSP script-src에서 `'unsafe-inline'` 제거

`'strict-dynamic'`이 적용된 환경에서 `'unsafe-inline'`은 현대 브라우저에서 무시되지만,  
구형 브라우저에서는 fallback으로 활성화되어 XSS 방어를 희석시킵니다.

`'nonce-{random}'` + `'strict-dynamic'` + 도메인 allowlist 조합으로 충분합니다.

**파일**: [`frontend/src/middleware.ts:11`](../../frontend/src/middleware.ts)

---

### [MEDIUM-5] 공개 nearby API 좌표 범위 검증 누락

업로드 API에는 좌표 범위 검증이 있었으나, 공개 조회 API에는 없었습니다.  
잘못된 좌표로 DB 함수 오류나 예외 스택 트레이스 노출 가능성이 있었습니다. HIGH-2 수정 시 함께 처리.

---

### [MEDIUM-6] GoogleMapsConfig @Setter 제거 → 생성자 바인딩

`@ConfigurationProperties` 바인딩을 위해 `@Setter`를 사용하고 있었습니다.  
코드 내에도 `TODO: 얘 안쓸 방법 찾아보자` 주석이 남아 있었습니다.

`@ConstructorBinding` + 생성자 주입으로 불변 객체로 전환.  
`@Configuration`을 제거하고 `SnapguideApplication`에 `@EnableConfigurationProperties(GoogleMapsConfig.class)` 추가.

**파일**
- [`GoogleMapsConfig.java`](../../src/main/java/yeonjae/snapguide/service/config/GoogleMapsConfig.java)
- [`SnapguideApplication.java`](../../src/main/java/yeonjae/snapguide/SnapguideApplication.java)

---

## LOW — 1건 (수정 완료)

### [LOW-3] application-local.yml 하드코딩 경로

`/Users/kim-yeonjae/Desktop/Study/snapguide/uploads` 절대 경로가 커밋되어 있었습니다.  
다른 개발자 환경에서 경로가 달라 즉시 동작하지 않는 문제입니다.

`${UPLOAD_BASE_DIR:${user.home}/snapguide/uploads}` 로 교체.  
환경변수 `UPLOAD_BASE_DIR`을 설정하면 우선 적용되고, 없으면 `~/snapguide/uploads`를 기본값으로 사용합니다.

**파일**: [`src/main/resources/application-local.yml`](../../src/main/resources/application-local.yml)

---

## 미수정 (기존 이슈 — 별도 작업 필요)

아래 항목은 이번 리뷰에서 식별되었으나 이전 리뷰(`auth-token-review.md`)와 중복되거나  
별도 설계 결정이 필요해 이번 커밋에 포함하지 않았습니다.

| 우선순위 | 이슈 | 참조 |
|---------|------|------|
| P1 | Refresh token rotation 미적용 | `auth-token-review.md#P1` |
| P2 | `@Indexed` → refresh token 원문 Redis 노출 | `auth-token-review.md#P2` |
| LOW | `JwtTokenProvider.createAccessToken` 프로덕션 미사용 (테스트만) | 삭제 시 테스트 수정 필요 |
| LOW | 다수 TODO/HACK/FIXME 주석 → 이슈 트래커 이관 권장 | — |
