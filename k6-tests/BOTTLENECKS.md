# 🔍 SnapGuide 성능 병목 분석 리포트

코드 분석을 통해 발견된 성능 병목 지점과 해결 방안

---

## 📊 발견된 병목 지점 (우선순위순)

### 🔴 1. 파일 업로드 동기 처리 (최우선)

**위치**: `MediaService.saveAll()` - Line 37-70

**문제**:
```java
for (MultipartFile file : files) {  // ← 순차 처리
    // 1. HEIC → JPG 변환 (CPU 집약)
    UploadFileDto savedFile = fileStorageService.uploadFile(file);

    // 2. EXIF 메타데이터 추출
    MediaMetaData metaData = mediaMetaDataService.extractAndSave(...);

    // 3. Google Maps API 호출 (blocking!)
    Location location = locationServiceGeoImpl
        .extractAndResolveLocation(...).block();  // ← 대기

    // 4. DB 저장
    mediaRepository.save(media);
}
```

**영향도**: ⭐⭐⭐⭐⭐
- 사진 1개당 2~5초 소요
- 10개 업로드 시 20~50초 대기
- 사용자 이탈 가능성 높음

**해결 방안**:

#### Option 1: 비동기 처리 (권장)
```java
@Service
public class MediaService {
    @Async
    public CompletableFuture<Long> saveMediaAsync(MultipartFile file) {
        // 각 파일을 병렬로 처리
        return CompletableFuture.supplyAsync(() -> {
            // 업로드 로직
        });
    }

    public List<Long> saveAll(List<MultipartFile> files) {
        List<CompletableFuture<Long>> futures = files.stream()
            .map(this::saveMediaAsync)
            .toList();

        return futures.stream()
            .map(CompletableFuture::join)
            .toList();
    }
}
```

**기대 효과**: 10개 파일 업로드 시간 50초 → 5초 (10배 개선)

#### Option 2: 메시지 큐 사용 (프로덕션)
```java
@Service
public class MediaService {
    private final RabbitTemplate rabbitTemplate;

    public Long saveAllAsync(List<MultipartFile> files) {
        String taskId = UUID.randomUUID().toString();

        // 큐에 작업 전송
        files.forEach(file ->
            rabbitTemplate.convertAndSend("media.upload", file)
        );

        return taskId; // 즉시 응답
    }
}
```

**기대 효과**: 즉시 응답 (백그라운드 처리)

---

### 🟡 2. Redis 캐싱 미사용

**위치**: 전체 Service 계층

**문제**:
- Redis 설정은 있지만 `@Cacheable` 미사용
- 동일한 가이드를 100번 조회하면 DB 쿼리 100번 발생

**영향도**: ⭐⭐⭐⭐
- 조회 API 성능 저하
- DB 부하 증가
- 확장성 제한

**해결 방안**:

```java
// 1. 캐시 설정 추가
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10)); // 10분 TTL

        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}

// 2. Service에 캐싱 적용
@Service
public class GuideService {
    @Cacheable(value = "guides", key = "#id")
    public GuideResponseDto getGuide(Long id) {
        // 캐시 히트 시 DB 조회 생략
    }

    @Cacheable(value = "guidesList", key = "'all'")
    public List<GuideResponseDto> getAllGuides() {
        // ...
    }

    @CacheEvict(value = "guides", key = "#guide.id")
    public void updateGuide(Guide guide) {
        // 업데이트 시 캐시 무효화
    }
}

// 3. Location 캐싱 (Google API 호출 감소)
@Service
public class ReverseGeocodingService {
    @Cacheable(value = "locations", key = "#lat + '_' + #lng")
    public Mono<Location> reverseGeocode(double lat, double lng) {
        // 동일 좌표는 캐시에서 반환
    }
}
```

**기대 효과**:
- API 조회: 500ms → 50ms (10배 개선)
- DB 부하: 90% 감소 (캐시 히트율에 따라)
- Google API 호출: 80% 감소

---

### 🟡 3. Google Maps API Blocking 호출

**위치**: `LocationServiceGeoImpl.java:40`

**문제**:
```java
Location location = reverseGeocodingService
    .reverseGeocode(lat, lng)
    .block();  // ← WebClient를 blocking으로 사용!
```

코드에 TODO 주석도 있음:
```java
/**
 * TODO
 * 🔎 block()의 위험성
 * - block()은 Reactive 흐름을 막고 동기식으로 대기
 * - 웹 요청 쓰레드에서 사용할 경우 성능 저하 및 deadlock 위험
 */
```

**영향도**: ⭐⭐⭐⭐
- Google API 응답 시간만큼 쓰레드 블로킹
- 동시 업로드 시 대기열 발생

**해결 방안**:

#### Option 1: 완전 비동기 처리
```java
@Service
public class LocationServiceGeoImpl {
    public CompletableFuture<Location> extractAndResolveLocationAsync(
        byte[] imageBytes
    ) {
        Optional<double[]> coordinate =
            ExifCoordinateExtractor.extractCoordinate(...);

        if (coordinate.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        double[] latLng = coordinate.get();

        // 캐시 확인
        List<Location> cached =
            locationRepository.findLocationByCoordinateNative(...);
        if (!cached.isEmpty()) {
            return CompletableFuture.completedFuture(cached.get(0));
        }

        // Mono를 CompletableFuture로 변환
        return reverseGeocodingService
            .reverseGeocode(latLng[0], latLng[1])
            .toFuture()  // ← block() 대신 toFuture()
            .thenApply(locationRepository::save);
    }
}
```

**기대 효과**: API 대기 시간 동안 다른 작업 처리 가능

---

### 🟢 4. PostGIS 공간 인덱스 확인 필요

**위치**: `location` 테이블

**문제**:
- ST_DWithin 쿼리 사용 중
- GIST 인덱스 유무 불확실

**영향도**: ⭐⭐⭐
- 반경 검색 시 Full Scan 가능성
- 데이터 증가 시 성능 급격히 저하

**확인 방법**:
```sql
-- PostgreSQL 접속
\d location

-- 인덱스 확인
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'location';
```

**해결 방안**:
```sql
-- GIST 인덱스 생성 (없다면)
CREATE INDEX IF NOT EXISTS idx_location_coordinate
ON location USING GIST(coordinate);

-- 테이블 분석 (쿼리 플래너 최적화)
ANALYZE location;

-- 인덱스 사용 확인
EXPLAIN ANALYZE
SELECT * FROM location
WHERE ST_DWithin(
    coordinate,
    ST_SetSRID(ST_MakePoint(126.9780, 37.5665), 4326),
    5000
);
```

**기대 효과**:
- 공간 쿼리: 1000ms → 300ms (3배 개선)
- 데이터 1만 건 이상에서 효과 극대화

---

### 🟢 5. 페이지네이션 부재

**위치**: `MediaService.getAllMedia()` - Line 77-82

**문제**:
```java
public List<MediaDto> getAllMedia() {
    return mediaRepository.findAll()  // ← 전체 조회!
        .stream()
        .map(MediaMapper::toDto)
        .collect(Collectors.toList());
}
```

**영향도**: ⭐⭐
- 데이터 1만 건 시 메모리 오버헤드
- 응답 시간 증가

**해결 방안**:
```java
public Page<MediaDto> getAllMedia(Pageable pageable) {
    return mediaRepository.findAll(pageable)
        .map(MediaMapper::toDto);
}

// 컨트롤러
@GetMapping
public ResponseEntity<Page<MediaDto>> getAllMedia(
    @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
) {
    return ResponseEntity.ok(mediaService.getAllMedia(pageable));
}
```

**기대 효과**: 메모리 사용량 90% 감소

---

## 📈 성능 개선 로드맵

### Phase 1: Quick Wins (1-2일)

- [x] 코드 분석 완료
- [ ] Redis 캐싱 추가 (GuideService, LocationService)
- [ ] PostGIS 인덱스 확인/생성
- [ ] 페이지네이션 적용

**예상 효과**: 조회 API 3배 개선

---

### Phase 2: Core Optimization (3-5일)

- [ ] 파일 업로드 비동기 처리
- [ ] Google Maps API 비동기 전환
- [ ] 에러 처리 강화

**예상 효과**: 업로드 10배 개선

---

### Phase 3: Advanced (1주)

- [ ] RabbitMQ 또는 Kafka 도입 (백그라운드 작업)
- [ ] CDN 연동 (이미지 서빙)
- [ ] DB 커넥션 풀 튜닝

**예상 효과**: 전체적인 확장성 향상

---

## 🎯 목표 성능 지표

| 메트릭 | 현재 (예상) | 목표 | 개선율 |
|--------|-------------|------|--------|
| 파일 업로드 p95 | 10초 | 2초 | 5배 |
| API 조회 p95 | 500ms | 50ms | 10배 |
| 공간 쿼리 p95 | 1000ms | 300ms | 3배 |
| 전체 처리량 | 50 req/s | 200 req/s | 4배 |
| 에러율 | 1% | 0.1% | 10배 개선 |

---

## 🔧 개선 후 검증 방법

### 1. k6 부하 테스트
```bash
# 베이스라인 측정
./k6-tests/run-all-tests.sh baseline

# 개선 후 측정
./k6-tests/run-all-tests.sh optimized

# 결과 비교
./k6-tests/run-all-tests.sh compare
```

### 2. Grafana 모니터링
- CPU 사용률 감소 확인
- 메모리 사용 패턴 개선
- 응답 시간 히스토그램

### 3. 프로덕션 검증
- 실제 사용자 피드백
- 이탈률 감소
- 페이지 로딩 속도 개선

---

## 📚 참고 자료

- [Spring Boot Async Processing](https://spring.io/guides/gs/async-method/)
- [Redis Caching with Spring](https://spring.io/guides/gs/caching/)
- [PostGIS Performance Tips](https://postgis.net/workshops/postgis-intro/indexing.html)
- [WebClient vs RestTemplate](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)

---

**분석 완료일**: 2025-12-03
**다음 리뷰**: 개선 완료 후
