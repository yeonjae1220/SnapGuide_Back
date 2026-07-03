# Changelog

이 프로젝트의 모든 주요 변경 사항을 기록합니다.

형식은 [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)를 따르며,
[유의적 버전(SemVer)](https://semver.org/lang/ko/)을 준수합니다.

## [Unreleased]

## [1.3.0] - 2026-07-01

공개 지도 탐색과 피드-지도 분리, 보안·관측성 강화에 집중한 릴리스.

### Added
- 비인증 사용자의 공개 지도 탐색 허용, 피드와 지도 탐색을 분리
- `lab.mungji` 콘솔 집계용 내부 엔드포인트
- JWT `userId` MDC + logstash JSON 구조화 로그, HTTP 요청당 1줄 access log
- 개발자 피드백 수집 엔드포인트 (토큰 검증 + NetworkPolicy)

### Security
- 관리자 formLogin 무차별 대입 lockout 추가 (GLOBAL-PIT-038)
- 회원가입 비밀번호 정책에 특수문자 필수 추가
- `/api/feedback`를 JWT 필터 화이트리스트에 추가 (비인증 접근 허용)

### Fixed
- login이 signup 비밀번호 복잡도 정책을 강제하던 회귀 수정 — 기존 회원 로그인 잠김 방지 (GLOBAL-PIT-044)
- 공개 피드 rate limit 분리 및 미디어 없는 가이드 제외
- 관리자 회원탭 `LazyInitializationException` 수정
- nearby guide 캐시 역직렬화 허용

## [1.2.0] - 2026-06-08

프론트엔드를 **Next.js로 마이그레이션**하고, 지도 기능과 인프라 보안을 대폭 강화한 릴리스.

### Added
- **프론트엔드 마이그레이션** — Next.js 전환 + 관리자 보안 강화 + CI/CD 개선
- SSR 관리자 패널 완성 — 데이터 주입, 템플릿 분리, 통계 확장
- **구글맵 가이드 위치 핀 마커 + 클러스터링 + 줌 자동 재조회**
- 줌아웃 시 국가/대륙 집계 오버레이, 경도 정규화
- SEO — OG 태그, sitemap, robots.txt
- NetworkPolicy — default-deny-all + 컴포넌트별 allowlist
- 라이트/다크 테마 지원
- k8s liveness/readiness probe를 httpGet으로 전환 + health 엔드포인트

### Changed
- Next.js 14 → 16 업그레이드 (HIGH 취약점 4건 해결), eslint 8 → 10
- Redis 커스텀 ConnectionFactory 제거, Spring Boot auto-config에 위임

### Security
- nonce 기반 CSP 완성 — strict-dynamic, base-uri, layout nonce (middleware 적용)
- refresh token을 Redis에 SHA-256 해시로 저장 (GLOBAL-PIT-001)
- 관리자 인증 취약점 전체 수정, credential 검증·세션 설정 하드닝
- Google Maps Script nonce 명시 적용, IP 주소 로그 제거

### Fixed
- Next.js hydration 스크립트 및 Google Maps용 CSP 허용, OAuth 로딩 플래시 수정
- IP 기반 rate limit이 프록시 환경에서 단일 버킷화되던 문제 수정 (GLOBAL-PIT-009)
- Redis 캐시 타입 역직렬화 500, 클라이언트 geo-IP 폴백, 집계 모드 엣지 케이스 수정
- PVC `storageClassName` 지정으로 immutable spec 충돌 해결

## [1.1.0] - 2026-05-10

관리자 기능과 미디어 업로드 성능, 쿠키 기반 세션을 도입한 릴리스.

### Added
- 관리자 API 엔드포인트 + 가이드/위치/댓글 CRUD + 보안 하드닝
- 별도 관리자 페이지 분리 (내비게이션에서 admin 탭 제거)
- 파일별 트랜잭션 격리 병렬 업로드 (batch parallel upload)
- 이미지 슬라이더가 있는 가이드 상세 모달
- EXIF 표시 및 위치 프라이버시 토글
- 위치를 행정구역(admin district)으로 표시, 좌표는 선택적
- geocoding API 키를 Maps JS API 키와 분리
- 회원가입 시 비밀번호 조건 실시간 인디케이터

### Changed
- refresh token을 httpOnly 쿠키로 이전, TTL 30일로 연장

### Security
- 만료 토큰 로그아웃, rate guard, CSRF 헤더, 보안 쿠키 opt-out 패턴 추가

### Fixed
- 갤러리 사진에서 GPS 위치가 추출되지 않던 문제 및 업로드 견고성 개선
- `locationName`이 항상 null이던 문제, 캐시 포이즈닝 수정
- 모바일 업로드/삭제/위치 버그 4건, 반응형 레이아웃 개선
- Google Maps `NotLoadingAPIFromGoogleMapsError` 및 CSP 이슈 해결
- 회원 탈퇴 실패 및 Google 계정 선택 강제 수정

## [1.0.0] - 2026-03-16

### Security
- OWASP 보안 취약점 대응
- 하드코딩된 Grafana 비밀번호를 환경변수로 교체
- 중요 설정 값 은닉 및 전반적 보안 태세 개선

### Added
- PWA 지원 — 오프라인 모드, 설치 프롬프트, Web Push 알림
- 썸네일용 S3 presigned URL 지원 및 AWS S3 스토리지 통합 (Docker 지원)
- 랜딩/메인 뷰 분리 프론트엔드 리디자인
- Redis 캐싱, 커서 기반 페이지네이션, 인증 개선
- 회원 탈퇴 시 가이드·스토리지 데이터 연쇄 정리
- 현재 위치 기반 및 지도 기반 검색, 좋아요 기능
- 가이드 저장·조회·수정·삭제, bounding box 기반 거리 조회
- 서버 사이드 HEIC → JPG 변환, 사진+팁 업로드, 대용량 파일 I/O
- JWT 기반 회원가입/로그인, Redis 토큰 블랙리스트 로그아웃
- [SNAP-11] Google OAuth2 로그인 연동
- k6 부하 테스트 스크립트 및 성능 모니터링 (Prometheus, Grafana, OpenTelemetry)
- 위치 기반 기능을 위한 PostGIS 공간 쿼리 지원

### Changed
- 비동기 파일 업로드 및 원자적 좋아요 카운트 최적화
- 서비스·스토리지 레이어 전반에 SOLID 원칙 및 디자인 패턴 적용, 결합도 감소
- 프로젝트 전반 클린 코드 개선 및 패키지 재구조화, EXIF 추출기 리팩터링
- JWT 에러 처리에 CustomException 적용, 헤더 검증 개선
- Google OAuth2 플로우에서 세션 로그인 → 쿠키 로그인 전환
- 토큰 전달을 모바일 인가 지원 1회용 코드 방식으로 변경

### Fixed
- REST API 계약 위반 수정
- JPA 안티패턴 (N+1, 지연 로딩, 트랜잭션 경계) 수정
- 웹 브라우저 Google OAuth 로그인 문제, HEIC 파일 타입 감지/처리 수정
- 다중 이미지 로딩 시 broken pipe 오류, 빈 간 순환 의존성 수정
- Docker 환경 YAML 들여쓰기 오류로 인한 설정 오적용 수정

[Unreleased]: https://github.com/yeonjae1220/SnapGuide_Back/compare/v1.3.0...HEAD
[1.3.0]: https://github.com/yeonjae1220/SnapGuide_Back/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/yeonjae1220/SnapGuide_Back/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/yeonjae1220/SnapGuide_Back/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/yeonjae1220/SnapGuide_Back/releases/tag/v1.0.0
