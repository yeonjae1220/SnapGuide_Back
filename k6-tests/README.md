# SnapGuide k6 부하 테스트 가이드

이 디렉토리는 SnapGuide 프로젝트의 성능 테스트와 병목 지점 발견을 위한 k6 부하 테스트 스크립트를 포함합니다.

## 📋 목차

1. [사전 준비](#사전-준비)
2. [테스트 시나리오](#테스트-시나리오)
3. [실행 방법](#실행-방법)
4. [성능 개선 과정](#성능-개선-과정)
5. [결과 분석](#결과-분석)

---

## 🛠️ 사전 준비

### 1. k6 설치

**macOS (Homebrew):**
```bash
brew install k6
```

**Linux:**
```bash
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

**Windows (Chocolatey):**
```bash
choco install k6
```

### 2. 애플리케이션 실행

```bash
# 애플리케이션 빌드 및 실행
./gradlew bootRun

# 또는 Docker로 실행
docker-compose up -d
```

### 3. 테스트 데이터 준비

프로젝트에 최소한의 테스트 데이터가 있어야 합니다:
- 회원 5명 이상
- 위치 정보 10개 이상
- 가이드 20개 이상

InitTestData를 통해 더미 데이터를 생성하거나, 직접 데이터를 추가하세요.

---

## 🎯 테스트 시나리오

### 1. 파일 업로드 테스트 (`1-upload-test.js`)

**목적**: 가장 큰 병목인 파일 업로드 성능 측정

**테스트 내용**:
- HEIC → JPG 변환
- 썸네일 생성
- EXIF 메타데이터 추출
- Google Maps API 호출 (위치 정보)
- DB 저장

**예상 병목**:
- ❌ 동기식 처리로 인한 지연
- ❌ Google API `.block()` 호출
- ❌ CPU 집약적 이미지 변환

### 2. API 읽기 테스트 (`2-api-read-test.js`)

**목적**: 조회 API 성능 및 캐싱 효과 측정

**테스트 내용**:
- 전체 가이드 목록 조회
- 단건 가이드 상세 조회
- 사용자별 가이드 조회

**예상 병목**:
- ❌ 캐싱 미사용으로 매번 DB 조회
- ❌ N+1 쿼리 문제 (이미 해결됨)

### 3. 공간 쿼리 테스트 (`3-spatial-query-test.js`)

**목적**: PostGIS 공간 검색 성능 측정

**테스트 내용**:
- ST_DWithin 반경 검색
- 여러 반경 크기 테스트 (1km, 3km, 5km, 10km)

**예상 병목**:
- ⚠️ GIST 인덱스 부재 가능성
- ⚠️ 큰 반경 검색 시 성능 저하

### 4. 혼합 시나리오 테스트 (`4-mixed-scenario-test.js`)

**목적**: 실제 사용자 행동 패턴 시뮬레이션

**테스트 내용**:
- 70% 읽기 (조회, 검색)
- 20% 쓰기 (좋아요, 댓글)
- 10% 업로드

**예상 병목**:
- 동시 접속자 증가 시 전체적인 성능 저하

---

## 🚀 실행 방법

### 기본 실행

```bash
# 1. 파일 업로드 테스트
k6 run k6-tests/scripts/1-upload-test.js

# 2. API 읽기 테스트
k6 run k6-tests/scripts/2-api-read-test.js

# 3. 공간 쿼리 테스트
k6 run k6-tests/scripts/3-spatial-query-test.js

# 4. 혼합 시나리오 테스트
k6 run k6-tests/scripts/4-mixed-scenario-test.js
```

### 결과를 파일로 저장

```bash
k6 run k6-tests/scripts/1-upload-test.js --out json=k6-tests/results/upload-baseline.json

# 성능 개선 후 다시 실행
k6 run k6-tests/scripts/1-upload-test.js --out json=k6-tests/results/upload-optimized.json
```

### 환경 변수 설정

```bash
# 다른 서버 테스트
k6 run -e BASE_URL=http://localhost:8082 k6-tests/scripts/2-api-read-test.js

# 캐싱 활성화 후 테스트
k6 run -e CACHE_ENABLED=true k6-tests/scripts/2-api-read-test.js

# 인증 토큰 사용
k6 run -e AUTH_TOKEN=your-jwt-token k6-tests/scripts/2-api-read-test.js
```

### 실시간 모니터링과 함께 실행

터미널을 2개 띄워서:

**터미널 1 - Grafana 모니터링:**
```bash
# Grafana 접속
open http://localhost:3000

# 또는 애플리케이션 로그 확인
docker-compose logs -f snapguide
```

**터미널 2 - k6 테스트:**
```bash
k6 run k6-tests/scripts/4-mixed-scenario-test.js
```

---

## 📈 성능 개선 과정

### Step 1: 베이스라인 측정

```bash
# 현재 상태 측정
k6 run k6-tests/scripts/1-upload-test.js --out json=results/baseline-upload.json
k6 run k6-tests/scripts/2-api-read-test.js --out json=results/baseline-api.json
k6 run k6-tests/scripts/3-spatial-query-test.js --out json=results/baseline-spatial.json
```

**예상 결과**:
- 파일 업로드: p95 > 10초
- API 조회: p95 > 500ms (캐시 없음)
- 공간 쿼리: p95 > 1초 (인덱스 확인 필요)

### Step 2: 병목 지점 개선

#### 개선 1: Redis 캐싱 추가

```java
// GuideService.java에 추가
@Cacheable(value = "guides", key = "#id")
public GuideResponseDto getGuide(Long id) {
    // ...
}

@Cacheable(value = "guidesList", key = "'all'")
public List<GuideResponseDto> getAllGuides() {
    // ...
}
```

**테스트**:
```bash
k6 run -e CACHE_ENABLED=true k6-tests/scripts/2-api-read-test.js
```

**기대 효과**: API 조회 p95 < 100ms (5배 이상 개선)

#### 개선 2: 파일 업로드 비동기 처리

```java
// MediaService.java
@Async
public CompletableFuture<Long> saveMediaAsync(MultipartFile file) {
    // 비동기 처리
}
```

**테스트**:
```bash
k6 run k6-tests/scripts/1-upload-test.js
```

**기대 효과**: 업로드 응답시간 p95 < 2초 (5배 이상 개선)

#### 개선 3: Google Maps API 캐싱

```java
@Cacheable(value = "locations", key = "#lat + '_' + #lng")
public Mono<Location> reverseGeocode(double lat, double lng) {
    // 동일 좌표는 캐시에서 반환
}
```

**기대 효과**: 중복 좌표 조회 시 API 호출 0회

#### 개선 4: PostGIS 인덱스 추가

```sql
-- PostgreSQL에서 실행
CREATE INDEX IF NOT EXISTS idx_location_coordinate
ON location USING GIST(coordinate);

-- 기존 데이터 분석
ANALYZE location;
```

**테스트**:
```bash
k6 run k6-tests/scripts/3-spatial-query-test.js
```

**기대 효과**: 공간 쿼리 p95 < 300ms (3배 이상 개선)

### Step 3: 최종 성능 검증

```bash
# 모든 개선 후 전체 시나리오 테스트
k6 run k6-tests/scripts/4-mixed-scenario-test.js --out json=results/optimized-mixed.json

# 베이스라인과 비교
k6 compare results/baseline-mixed.json results/optimized-mixed.json
```

---

## 📊 결과 분석

### 주요 메트릭

| 메트릭 | 목표 | 설명 |
|--------|------|------|
| `http_req_duration` p95 | < 500ms | 95%의 요청이 0.5초 이내 |
| `http_req_duration` p99 | < 1s | 99%의 요청이 1초 이내 |
| `http_req_failed` | < 1% | 에러율 1% 미만 |
| `http_reqs` | - | 초당 처리 요청 수 |

### 성능 개선 체크리스트

- [ ] **캐싱 추가**: API 조회 응답 시간 5배 개선
- [ ] **비동기 처리**: 파일 업로드 응답 시간 5배 개선
- [ ] **인덱스 최적화**: 공간 쿼리 3배 개선
- [ ] **페이지네이션**: 대용량 조회 시 메모리 사용량 감소
- [ ] **연결 풀 튜닝**: DB 연결 대기 시간 감소

### Grafana 대시보드 확인

1. **JVM 메트릭**:
   - Heap Memory 사용량
   - GC 빈도 및 시간
   - Thread Pool 상태

2. **HTTP 메트릭**:
   - 요청 처리 시간
   - 에러율
   - 동시 접속자 수

3. **Database 메트릭**:
   - 커넥션 풀 사용률
   - 쿼리 실행 시간
   - Slow Query 로그

4. **Traces**:
   - 느린 요청 추적
   - 병목 지점 식별

---

## 🔧 트러블슈팅

### 문제 1: k6에서 파일 업로드가 실패합니다

**원인**: 더미 이미지가 너무 작거나 서버에서 거부됨

**해결**:
```bash
# 실제 테스트 이미지 준비
cp your-test-image.jpg k6-tests/data/test-image.jpg

# 스크립트에서 실제 파일 사용
```

### 문제 2: 모든 요청이 실패합니다

**원인**: 애플리케이션이 실행되지 않았거나 포트가 다름

**해결**:
```bash
# 애플리케이션 상태 확인
curl http://localhost:8080/actuator/health

# 포트 확인 후 BASE_URL 수정
k6 run -e BASE_URL=http://localhost:8082 k6-tests/scripts/2-api-read-test.js
```

### 문제 3: 공간 쿼리가 너무 느립니다

**원인**: GIST 인덱스가 없음

**해결**:
```sql
-- PostgreSQL 접속
psql -h localhost -U postgres -d snapguidedb

-- 인덱스 확인
\d location

-- 인덱스 생성
CREATE INDEX idx_location_coordinate ON location USING GIST(coordinate);
```

---

## 📚 참고 자료

- [k6 공식 문서](https://k6.io/docs/)
- [k6 메트릭 이해하기](https://k6.io/docs/using-k6/metrics/)
- [Spring Boot 성능 튜닝 가이드](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [PostGIS 인덱스 최적화](https://postgis.net/workshops/postgis-intro/indexing.html)

---

## 📝 다음 단계

1. ✅ **베이스라인 측정 완료**
2. 🔄 **병목 지점 개선 중**
3. ⏳ **최종 검증 대기**
4. ⏳ **프로덕션 배포**

**Happy Load Testing! 🚀**
