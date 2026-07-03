# SnapGuide

사진 기반 여행 가이드 공유 플랫폼

## 소개

SnapGuide는 사용자들이 사진과 함께 여행 팁을 공유하고, 위치 기반으로 다른 사용자의 가이드를 발견할 수 있는 애플리케이션입니다. 사진의 EXIF 메타데이터를 자동 추출하여 촬영 위치와 카메라 정보를 활용하고, PostGIS 기반의 공간 쿼리로 근처 가이드를 검색할 수 있습니다.

## 주요 기능

- **여행 가이드 공유**: 사진과 함께 여행 팁 작성, 조회, 수정, 삭제
- **위치 기반 검색**: 현재 위치 반경 또는 지도 범위 기반으로 근처 가이드 발견
- **지도 탐색**: 구글맵 가이드 핀 마커 · 클러스터링 · 줌아웃 시 국가/대륙 집계 오버레이 (비인증 공개 탐색 지원)
- **관리자 패널**: SSR 기반 가이드/위치/댓글 관리 (formLogin lockout)
- **EXIF 메타데이터 추출**: 사진에서 GPS 좌표, 촬영 시간, 카메라 정보 자동 추출 (위치 프라이버시 토글)
- **미디어 처리**: 썸네일 자동 생성, HEIC→JPEG 변환, S3 Presigned URL 지원, 비동기 업로드
- **소셜 로그인**: Google OAuth2 인증, 토큰 블랙리스트 기반 로그아웃, 회원 탈퇴 및 데이터 삭제
- **좋아요 기능**: 가이드 좋아요 및 인기순 정렬 (원자적 카운트 최적화)
- **PWA 지원**: 오프라인 모드, 홈 화면 설치 프롬프트, Web Push 알림
- **커서 기반 페이지네이션**: 대용량 데이터 효율적 조회
- **Redis 캐싱**: 조회 성능 최적화 및 세션 관리

## 기술 스택

### Backend
| 구분 | 기술 |
|---|---|
| Framework | Spring Boot 3.4.5 (Java 17) |
| Database | PostgreSQL 15 + PostGIS |
| Cache | Redis 7 |
| ORM | Spring Data JPA + QueryDSL |
| Security | OAuth2 (Google) + JWT |
| Storage | AWS S3 / Local / NAS |
| API Docs | SpringDoc OpenAPI (Swagger) |

### Frontend
| 구분 | 기술 |
|---|---|
| Framework | Next.js (App Router) · nonce 기반 CSP |
| 지도 | Google Maps — 가이드 위치 핀 마커 · 클러스터링 · 줌 집계 오버레이 |
| PWA | Service Worker, Web App Manifest, 오프라인(Cache API) |
| 알림 | Web Push Notifications |

> 프론트엔드는 v1.2.0에서 정적 SPA → Next.js App Router로 마이그레이션되었습니다.

### DevOps & Observability
| 구분 | 기술 |
|---|---|
| Container | Docker, Docker Compose |
| Orchestration | Kubernetes (k8s) |
| Tracing | OpenTelemetry + Grafana Tempo |
| Metrics | Prometheus + Micrometer |
| Logging | Loki + Promtail (JSON 구조화 로깅) |
| Dashboard | Grafana |
| Load Testing | k6 |

## 프로젝트 구조

```
snapguide/
├── src/main/java/yeonjae/snapguide/
│   ├── controller/           # REST API 컨트롤러 (Swagger 문서화)
│   ├── domain/               # 도메인 엔티티
│   │   ├── member/           # 회원 (OAuth2, JWT)
│   │   ├── guide/            # 여행 가이드
│   │   ├── media/            # 미디어 파일 + EXIF 추출
│   │   ├── location/         # 위치 정보 (PostGIS)
│   │   ├── comment/          # 댓글
│   │   └── like/             # 좋아요
│   ├── service/              # 비즈니스 로직
│   ├── repository/           # JPA + QueryDSL 데이터 접근
│   ├── security/             # OAuth2 + JWT 인증/인가
│   └── infrastructure/       # AOP, Redis 캐시, AWS 설정
├── src/main/resources/
│   └── static/               # PWA (index.html, service-worker.js, manifest.json)
├── k6-tests/                 # 부하 테스트 시나리오
├── k8s/                      # Kubernetes 배포 매니페스트
└── db-migration/             # 데이터베이스 마이그레이션
```

## 시작하기

### 환경 변수 설정

`.env` 파일을 프로젝트 루트에 생성하고 아래 항목을 채워야 합니다.
자세한 내용은 [ENV_SETUP.md](ENV_SETUP.md)를 참고하세요.

```bash
DB_URL=jdbc:postgresql://localhost:5432/snapguidedb
DB_USERNAME=postgres
DB_PASSWORD=<your-password>
JWT_SECRET=<openssl rand -hex 64>
GOOGLE_CLIENT_ID=<your-google-client-id>
GOOGLE_CLIENT_SECRET=<your-google-client-secret>
GOOGLE_MAPS_API_KEY=<your-maps-api-key>
AWS_ACCESS_KEY=<your-aws-key>
AWS_SECRET_KEY=<your-aws-secret>
```

> **주의**: `.env` 파일은 절대 Git에 커밋하지 마세요.

### 로컬 실행

```bash
# 의존 서비스 시작 (PostgreSQL, Redis, Observability Stack)
docker-compose up -d

# 애플리케이션 빌드 및 실행
./gradlew bootRun
```

- 로컬 포트: **8080**
- Docker 포트: **8082**
- API 문서: http://localhost:8080/swagger-ui.html

### Observability 대시보드

| 서비스 | URL | 계정 |
|---|---|---|
| Grafana | http://localhost:3000 | admin / (환경변수) |
| Prometheus | http://localhost:9090 | - |
| Tempo | http://localhost:3200 | - |
| Loki | http://localhost:3100 | - |

자세한 내용은 [OBSERVABILITY.md](OBSERVABILITY.md)를 참고하세요.

## 테스트

```bash
# 전체 테스트 실행
./gradlew test

# 특정 클래스 테스트
./gradlew test --tests "ClassName"

# 부하 테스트 (k6)
cd k6-tests && k6 run scripts/1-upload-test.js
```

### k6 부하 테스트 시나리오

| 시나리오 | 파일 | 목적 |
|---|---|---|
| 파일 업로드 | `1-upload-test.js` | HEIC 변환, EXIF 추출, S3 업로드 성능 |
| API 읽기 | `2-api-read-test.js` | 조회 API 및 Redis 캐싱 효과 측정 |
| 공간 쿼리 | `3-spatial-query-test.js` | PostGIS ST_DWithin 반경 검색 성능 |
| 혼합 시나리오 | `4-mixed-scenario-test.js` | 실제 사용자 행동 패턴 시뮬레이션 (70% 읽기 / 20% 쓰기 / 10% 업로드) |

자세한 내용은 [k6-tests/README.md](k6-tests/README.md)를 참고하세요.

## 보안

- OWASP Top 10 취약점 대응
- 환경 변수 기반 민감 정보 관리 (하드코딩 없음)
- JWT 토큰 블랙리스트 (Redis 기반 로그아웃)
- HTTP-only 쿠키 기반 세션 관리
- OAuth2 One-Time Authorization Code 방식 (모바일 지원)

## 버전 히스토리

버전별 상세 변경 내역은 [CHANGELOG.md](CHANGELOG.md)를 참고하세요.

| 버전 | 날짜 | 핵심 변경 |
|------|------|-----------|
| 1.3.0 | 2026-07-01 | 공개 지도 탐색 · 피드-지도 분리 · admin 락아웃 · 구조화 로깅 |
| 1.2.0 | 2026-06-08 | **Next.js 마이그레이션** · **지도 핀/클러스터링** · SEO · NetworkPolicy |
| 1.1.0 | 2026-05-10 | 관리자 CRUD · 병렬 업로드 · httpOnly 쿠키 인증 |
| 1.0.0 | 2026-03-16 | PWA · S3 스토리지 · Google OAuth2 · PostGIS 공간 쿼리 |

## 서비스 접속

현재 프로젝트는 https://snapguide.mungji.com 에서 제공됩니다.

## 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.
