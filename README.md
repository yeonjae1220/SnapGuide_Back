# SnapGuide
사진 기반 여행 가이드 공유 플랫폼

## 소개
SnapGuide는 사용자들이 사진과 함께 여행 팁을 공유하고, 위치 기반으로 다른 사용자의 가이드를 발견할 수 있는 애플리케이션입니다. 사진의 EXIF 메타데이터를 자동 추출하여 촬영 위치와 카메라 정보를 활용하고, PostGIS 기반의 공간 쿼리로 근처 가이드를 검색할 수 있습니다.

## 주요 기능

- **여행 가이드 공유**: 사진과 함께 여행 팁 작성 및 공유
- **위치 기반 검색**: 반경 기반으로 근처 가이드 발견
- **EXIF 메타데이터 추출**: 사진에서 GPS 좌표, 촬영 시간, 카메라 정보 자동 추출
- **미디어 처리**: 썸네일 자동 생성, HEIC→JPEG 변환 지원, S3 Presigned URL 지원
- **소셜 로그인**: Google OAuth2 인증, 토큰 블랙리스트 기반 로그아웃
- **좋아요 기능**: 가이드 좋아요 및 인기순 정렬
- **PWA 지원**: 오프라인 지원, 홈 화면 설치, 푸시 알림

## 기술 스택

### Backend
| 구분 | 기술 |
|---|---|
| Framework | Spring Boot 3.4.5 (Java 17) |
| Database | PostgreSQL 15 + PostGIS |
| Cache | Redis 7 |
| ORM | Spring Data JPA + QueryDSL |
| Security | OAuth2 + JWT |
| Storage | AWS S3 / Local / NAS |

### DevOps & Observability
| 구분 | 기술 |
|---|---|
| Container | Docker, Docker Compose |
| Tracing | OpenTelemetry + Tempo |
| Metrics | Prometheus + Micrometer |
| Logging | Loki + Promtail |
| Dashboard | Grafana |
| Load Testing | k6 |

## 프로젝트 구조

```
src/main/java/yeonjae/snapguide/
├── controller/           # REST API 컨트롤러
├── domain/               # 도메인 엔티티
│   ├── member/           # 회원
│   ├── guide/            # 여행 가이드
│   ├── media/            # 미디어 파일
│   ├── location/         # 위치 정보
│   ├── comment/          # 댓글
│   └── like/             # 좋아요
├── service/              # 비즈니스 로직
├── repository/           # 데이터 접근 (JPA + QueryDSL)
├── security/             # OAuth2 + JWT 인증
└── infrastructure/       # AOP, Cache, AWS 설정
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
| Grafana | http://localhost:3000 | admin / admin |
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
cd k6-tests && k6 run <script.js>
```

부하 테스트 시나리오 및 결과 분석은 [k6-tests/README.md](k6-tests/README.md)를 참고하세요.

## 서비스 접속

현재 프로젝트는 https://briankim.synology.me/ 에서 제공됩니다.

- 현재 UI가 완성되지 않아 개발용 화면으로 이어집니다.
- 운영용 데이터가 아직 갖추어지지 않았습니다.

## 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.
