# SnapGuide
사진 기반 여행 가이드 공유 플랫폼

# 소개
SnapGuide는 사용자들이 사진과 함께 여행 팁을 공유하고, 위치 기반으로 다른 사용자의 가이드를 발견할 수 있는 애플리케이션입니다. 사진의 EXIF 메타데이터를 자동 추출하여 촬영 위치와 카메라 정보를 활용하고, PostGIS 기반의 공간 쿼리로 근처 가이드를 검색할 수 있습니다.

# 주요 기능
여행 가이드 공유: 사진과 함께 여행 팁 작성 및 공유
위치 기반 검색: 반경 기반으로 근처 가이드 발견
EXIF 메타데이터 추출: 사진에서 GPS 좌표, 촬영 시간, 카메라 정보 자동 추출
미디어 처리: 썸네일 자동 생성, HEIC→JPEG 변환 지원
소셜 로그인: Google OAuth2 인증
좋아요 기능: 가이드 좋아요 및 인기순 정렬

# 기술 스택
Backend
Framework: Spring Boot 3.4.5 (Java 17)
Database: PostgreSQL 15 + PostGIS
Cache: Redis 7
ORM: Spring Data JPA + QueryDSL
Security: OAuth2 + JWT
Storage: AWS S3, Local, NAS
DevOps & Observability
Container: Docker, Docker Compose
Tracing: OpenTelemetry + Tempo
Metrics: Prometheus + Micrometer
Logging: Loki + Promtail
Dashboard: Grafana

# 프로젝트 구조
~~~
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
~~~

# 서비스 접속
현재 프로젝트는 https://briankim.synology.me/ 에서 제공됩니다. 현재 UI가 완성되지 않아 개발용 화면으로 이어집니다.

# 라이선스
이 프로젝트는 MIT 라이선스 하에 배포됩니다.
