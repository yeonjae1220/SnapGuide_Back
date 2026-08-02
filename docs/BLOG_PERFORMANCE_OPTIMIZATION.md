# Spring Boot 성능 최적화: 비동기 파일 업로드와 원자적 좋아요 업데이트

> 실제 부하 테스트(k6)를 통해 발견한 병목 지점을 해결한 과정을 기록합니다.

---

## 목차
1. [문제 상황](#1-문제-상황)
2. [비동기 파일 업로드 최적화](#2-비동기-파일-업로드-최적화)
3. [원자적 좋아요 업데이트](#3-원자적-좋아요-업데이트)
4. [성능 테스트 결과](#4-성능-테스트-결과)
5. [핵심 교훈](#5-핵심-교훈)

---

## 1. 문제 상황

### 부하 테스트 환경
- **도구**: k6 (Grafana)
- **시나리오**: 70% 읽기 / 20% 좋아요 / 10% 파일 업로드
- **부하**: 최대 300 VUs, 10분간 테스트

### 발견된 병목 지점

1. **파일 업로드 응답 지연**: 한 장의 사진 업로드에 3~5초 소요
2. **좋아요 동시성 문제**: 동시 요청 시 Lost Update 발생 가능
3. **HikariCP 커넥션 고갈**: 파일 처리 중 DB 커넥션 장시간 점유

---

## 2. 비동기 파일 업로드 최적화

### 2.1 이전 구조 (동기 처리)

```
사용자 요청 ──────────────────────────────────────────────────▶ 응답
            │                                                    │
            ▼                                                    │
    ┌───────────────┐                                           │
    │ 원본 S3 업로드 │ ~800ms                                    │
    └───────────────┘                                           │
            │                                                    │
            ▼                                                    │
    ┌───────────────┐                                           │
    │ 웹용 JPG 생성  │ ~500ms                                    │
    └───────────────┘                                           │
            │                                                    │
            ▼                                                    │
    ┌───────────────┐                                           │
    │ 웹용 S3 업로드 │ ~600ms                                    │
    └───────────────┘                                           │
            │                                                    │
            ▼                                                    │
    ┌───────────────┐                                           │
    │ 썸네일 생성    │ ~300ms                                    │
    └───────────────┘                                           │
            │                                                    │
            ▼                                                    │
    ┌───────────────┐                                           │
    │ 썸네일 S3 업로드│ ~400ms                                    │
    └───────────────┘                                           │
            │                                                    │
            ▼                                                    │
    ┌───────────────┐                                           │
    │ DB 저장        │ ~50ms                                     │
    └───────────────┴────────────────────────────────────────────┘

    총 소요시간: 약 2,650ms (단일 파일 기준)
```

**문제점:**
- 사용자가 모든 이미지 처리가 완료될 때까지 대기
- 동기 처리 중 DB 커넥션 계속 점유
- S3 업로드 3회 → 네트워크 I/O 병목

### 2.2 개선된 구조 (비동기 처리)

```
사용자 요청 ────────────────────────▶ 응답 (즉시)
            │                         │
            ▼                         │
    ┌───────────────┐                │
    │ 원본 S3 업로드 │ ~800ms         │
    └───────────────┘                │
            │                         │
            ▼                         │
    ┌───────────────┐                │
    │ DB 저장        │ ~50ms          │
    └───────────────┴────────────────┘
            │
            │ (비동기 - 별도 스레드)
            ▼
    ┌─────────────────────────────────┐
    │  AsyncFileProcessingService     │
    │  ┌───────────────┐              │
    │  │ 웹용 JPG 생성  │              │
    │  └───────────────┘              │
    │  ┌───────────────┐              │
    │  │ 웹용 S3 업로드 │              │
    │  └───────────────┘              │
    │  ┌───────────────┐              │
    │  │ 썸네일 생성    │              │
    │  └───────────────┘              │
    │  ┌───────────────┐              │
    │  │ 썸네일 S3 업로드│              │
    │  └───────────────┘              │
    │  ┌───────────────┐              │
    │  │ DB URL 업데이트 │ (별도 트랜잭션)│
    │  └───────────────┘              │
    └─────────────────────────────────┘

    사용자 체감 응답시간: 약 850ms (68% 개선)
```

### 2.3 핵심 코드

#### AsyncConfig.java - 스레드 풀 설정
```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "fileProcessingExecutor")
    public Executor fileProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);      // 기본 스레드 4개
        executor.setMaxPoolSize(8);       // 최대 8개까지 확장
        executor.setQueueCapacity(100);   // 대기 큐 100개
        executor.setThreadNamePrefix("file-proc-");
        executor.setRejectedExecutionHandler((r, e) ->
            log.warn("File processing task rejected, queue full"));
        executor.initialize();
        return executor;
    }
}
```

#### S3FileStorageService.java - 원본만 동기 업로드
```java
@Override
public UploadFileDto uploadOriginalOnly(MultipartFile file) throws IOException {
    // 1. HEIC → JPG 변환 (필요시)
    byte[] fileBytes = file.getBytes();
    if (isHeicFormat(mimeType)) {
        fileBytes = convertHeicToJpg(fileBytes);
    }

    // 2. 원본만 S3 업로드 (1회만!)
    String originalKey = "images/originals/" + baseFileName + "." + extension;
    amazonS3.putObject(bucketName, originalKey,
        new ByteArrayInputStream(fileBytes), metadata);

    // 3. 원본 정보만 반환 (웹용/썸네일은 null)
    return UploadFileDto.builder()
            .originalFileBytes(fileBytes)
            .originalKey(originalKey)
            .baseFileName(baseFileName)
            .webDir(null)       // 비동기로 나중에 채워짐
            .thumbnailDir(null) // 비동기로 나중에 채워짐
            .build();
}
```

#### AsyncFileProcessingService.java - 파생 파일 비동기 생성
```java
@Service
public class AsyncFileProcessingService {

    /**
     * 비동기 파생 파일 생성 (트랜잭션 없음 - DB 커넥션 점유 최소화)
     */
    @Async("fileProcessingExecutor")
    public void generateDerivativesAsync(Long mediaId, String baseFileName, byte[] originalBytes) {
        log.info("[Async] Starting derivative generation for mediaId: {}", mediaId);

        try {
            // 1. 웹용 JPG 생성 & S3 업로드
            byte[] webBytes = createWebJpg(originalBytes);
            String webUrl = uploadToS3(webBytes, "images/web/" + baseFileName + ".jpg");

            // 2. 썸네일 생성 & S3 업로드
            byte[] thumbBytes = createThumbnail(webBytes);
            String thumbUrl = uploadToS3(thumbBytes, "images/thumbnails/" + baseFileName + ".jpg");

            // 3. DB 업데이트 (별도 트랜잭션 - 커넥션 점유 최소화)
            updateMediaUrls(mediaId, webUrl, thumbUrl);

        } catch (Exception e) {
            log.error("[Async] Failed for mediaId: {}", mediaId, e);
        }
    }

    /**
     * DB 업데이트만 트랜잭션으로 처리 (커넥션 점유 최소화)
     */
    @Transactional
    public void updateMediaUrls(Long mediaId, String webUrl, String thumbUrl,
                                 String webKey, String thumbnailKey) {
        mediaRepository.findById(mediaId).ifPresent(media -> {
            media.updateDerivativeUrls(webKey, thumbnailKey, webUrl);
            mediaRepository.save(media);
        });
    }
}
```

#### MediaService.java - 통합 업로드 로직
```java
public List<Media> saveAllAndGet(List<MultipartFile> files) throws IOException {
    List<Media> savedMediaList = new ArrayList<>();
    List<AsyncTask> asyncTasks = new ArrayList<>();

    for (MultipartFile file : files) {
        // 1. 원본만 빠르게 업로드 (동기)
        UploadFileDto savedFile = fileStorageService.uploadOriginalOnly(file);

        // 2. 메타데이터 & 위치 정보 추출
        MediaMetaData metaData = mediaMetaDataService.extractAndSave(savedFile.getOriginalFileBytes());
        Location location = locationServiceGeoImpl.extractAndResolveLocation(savedFile.getOriginalFileBytes());

        // 3. Media 엔티티 저장 (webKey, thumbnailKey는 null)
        Media media = Media.builder()
                .mediaName(file.getOriginalFilename())
                .originalKey(savedFile.getOriginalKey())
                .webKey(null)        // 비동기 처리 후 업데이트
                .thumbnailKey(null)  // 비동기 처리 후 업데이트
                .build();
        mediaRepository.save(media);
        savedMediaList.add(media);

        // 4. 비동기 작업 예약
        asyncTasks.add(new AsyncTask(media.getId(), savedFile.getBaseFileName(),
                                      savedFile.getOriginalFileBytes()));
    }

    // 5. 비동기 작업 시작 (사용자 응답 후 백그라운드 처리)
    for (AsyncTask task : asyncTasks) {
        asyncFileProcessingService.generateDerivativesAsync(
            task.mediaId, task.baseFileName, task.originalBytes);
    }

    return savedMediaList;
}
```

### 2.4 핵심 포인트

| 항목 | 이전 | 이후 |
|------|------|------|
| S3 업로드 횟수 | 3회 (원본+웹+썸네일) | 1회 (원본만) |
| 사용자 대기 시간 | ~2,650ms | ~850ms |
| DB 커넥션 점유 | 전체 처리 시간 | 저장 시점만 (~50ms) |
| 이미지 변환 | 동기 (블로킹) | 비동기 (논블로킹) |

---

## 3. 원자적 좋아요 업데이트

### 3.1 이전 구조 (동시성 문제 존재)

```java
@Transactional
public boolean toggleLike(Long guideId, UserDetails userDetails) {
    Guide guide = findGuide(guideId);      // SELECT 쿼리
    guide.increaseLikeCount();             // 메모리에서 +1
    // 트랜잭션 커밋 시 UPDATE (Dirty Checking)
}
```

**Lost Update 문제:**
```
시간 →
User A: SELECT (count=10) → +1 → UPDATE (count=11)
User B:      SELECT (count=10) → +1 → UPDATE (count=11)  ← 11이어야 하는데 11
                                                           (User A의 변경 손실!)
```

### 3.2 1차 개선: 원자적 UPDATE 쿼리

```java
// GuideRepository.java
@Modifying
@Query("UPDATE Guide g SET g.likeCount = g.likeCount + 1 WHERE g.id = :guideId")
void incrementLikeCount(@Param("guideId") Long guideId);

// GuideService.java
@Transactional
public boolean toggleLike(Long guideId, UserDetails userDetails) {
    if (!guideRepository.existsById(guideId)) {
        throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
    }

    Guide guideRef = guideRepository.getReferenceById(guideId);
    Optional<GuideLike> like = guideLikeRepository.findByMemberAndGuide(member, guideRef);

    if (like.isPresent()) {
        guideLikeRepository.delete(like.get());
        guideRepository.decrementLikeCount(guideId);  // DB에서 원자적 -1
        return false;
    } else {
        guideLikeRepository.save(new GuideLike(member, guideRef));
        guideRepository.incrementLikeCount(guideId);  // DB에서 원자적 +1
        return true;
    }
}
```

**원자적 UPDATE의 동작:**
```sql
-- 이 쿼리는 DB 레벨에서 원자적으로 실행됨
UPDATE guide SET like_count = like_count + 1 WHERE id = ?

시간 →
User A: UPDATE ... SET like_count = like_count + 1  → count=11
User B:      UPDATE ... SET like_count = like_count + 1  → count=12 ✓
```

### 3.3 문제 발생: 성능 2배 저하

부하 테스트 결과, 오히려 성능이 나빠졌습니다.

**원인 분석:**

1. **쿼리 수 증가**
   ```
   이전: SELECT 1회 + UPDATE 1회 (Dirty Checking) = 2쿼리
   이후: SELECT 2회 + DELETE/INSERT 1회 + UPDATE 1회 = 4쿼리
   ```

2. **`@Modifying` 옵션 누락**
   ```java
   @Modifying  // clearAutomatically, flushAutomatically 기본값 = false
   ```
   - `flushAutomatically = false`: INSERT/DELETE가 flush되지 않은 상태에서 UPDATE 실행
   - `clearAutomatically = false`: 영속성 컨텍스트 캐시 불일치

3. **프록시 초기화 오버헤드**
   ```java
   guideLikeRepository.findByMemberAndGuide(member, guideRef);
   // guideRef 프록시가 초기화되면서 추가 SELECT 발생 가능
   ```

### 3.4 최종 개선: 전체 최적화

#### GuideRepository.java - @Modifying 옵션 추가
```java
/**
 * 원자적 좋아요 증가 (동시성 안전)
 * - flushAutomatically: UPDATE 실행 전 영속성 컨텍스트 flush
 * - clearAutomatically: UPDATE 후 영속성 컨텍스트 clear
 */
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("UPDATE Guide g SET g.likeCount = g.likeCount + 1 WHERE g.id = :guideId")
void incrementLikeCount(@Param("guideId") Long guideId);

@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("UPDATE Guide g SET g.likeCount = g.likeCount - 1 WHERE g.id = :guideId AND g.likeCount > 0")
void decrementLikeCount(@Param("guideId") Long guideId);
```

#### GuideLikeRepository.java - ID 기반 쿼리 추가
```java
/**
 * ID 기반 좋아요 존재 여부 확인 (프록시 초기화 없음)
 */
boolean existsByMemberIdAndGuideId(Long memberId, Long guideId);

/**
 * ID 기반 좋아요 삭제 (SELECT 없이 바로 DELETE)
 */
@Modifying
@Query("DELETE FROM GuideLike gl WHERE gl.member.id = :memberId AND gl.guide.id = :guideId")
void deleteByMemberIdAndGuideId(@Param("memberId") Long memberId, @Param("guideId") Long guideId);
```

> **참고**: Spring Data JPA의 `deleteBy...` 메서드는 먼저 SELECT로 엔티티를 조회한 후 DELETE를 실행합니다.
> `@Query`로 직접 작성하면 SELECT 없이 바로 DELETE가 실행됩니다.

#### GuideService.java - 최적화된 toggleLike
```java
/**
 * 좋아요 토글 (원자적 업데이트 + 쿼리 최적화)
 *
 * 최적화 포인트:
 * 1. existsById 제거 → FK 제약조건이 guideId 유효성 검증
 * 2. ID 기반 쿼리 사용 → 프록시 초기화 없이 직접 쿼리
 * 3. @Modifying(flush, clear) → 영속성 컨텍스트 동기화
 */
@Transactional
public boolean toggleLike(Long guideId, UserDetails userDetails) {
    if (userDetails == null) {
        throw new IllegalArgumentException("로그인이 필요한 서비스입니다.");
    }

    Member member = memberRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

    // ID 기반 쿼리로 좋아요 존재 여부 확인 (프록시 초기화 없음)
    boolean likeExists = guideLikeRepository.existsByMemberIdAndGuideId(
        member.getId(), guideId);

    if (likeExists) {
        // 좋아요 취소
        guideLikeRepository.deleteByMemberIdAndGuideId(member.getId(), guideId);
        guideRepository.decrementLikeCount(guideId);  // 원자적 감소
        return false;
    } else {
        // 좋아요 추가 (getReferenceById: SELECT 없이 프록시만 생성)
        Guide guideRef = guideRepository.getReferenceById(guideId);
        guideLikeRepository.save(new GuideLike(member, guideRef));
        guideRepository.incrementLikeCount(guideId);  // 원자적 증가
        return true;
    }
}
```

### 3.5 쿼리 비교

| 시나리오 | 이전 (Dirty Checking) | 1차 개선 | 최종 최적화 |
|---------|---------------------|---------|-----------|
| 좋아요 추가 | SELECT 1 + UPDATE 1 | SELECT 2 + INSERT 1 + UPDATE 1 | SELECT 1 + INSERT 1 + UPDATE 1 |
| 좋아요 취소 | SELECT 1 + UPDATE 1 | SELECT 2 + DELETE 1 + UPDATE 1 | SELECT 1 + DELETE 1 + UPDATE 1 |
| 총 쿼리 수 | 2개 | 4개 | 3개 |
| 동시성 안전 | ❌ Lost Update | ✅ | ✅ |

---

## 4. 성능 테스트 결과

### k6 테스트 조건
```javascript
export const options = {
  stages: [
    { duration: '1m', target: 20 },    // 워밍업
    { duration: '3m', target: 100 },   // 일반 부하
    { duration: '3m', target: 200 },   // 피크 시간대
    { duration: '2m', target: 300 },   // 스트레스 테스트
    { duration: '1m', target: 0 },     // 종료
  ],
};
```

### 결과 요약

| 지표 | 최적화 전 | 최적화 후 | 개선율 |
|-----|----------|----------|-------|
| 평균 응답시간 | ~150ms | ~65ms | **57% 개선** |
| P95 응답시간 | ~800ms | ~430ms | **46% 개선** |
| 처리량 (RPS) | ~90 | ~128 | **42% 향상** |
| 업로드 응답시간 | ~2,600ms | ~850ms | **67% 개선** |

---

## 5. 핵심 교훈

### 5.1 비동기 처리 설계 원칙

1. **사용자 응답과 후속 처리 분리**
   - 사용자에게 즉시 필요한 것만 동기 처리
   - 나머지는 비동기로 백그라운드 처리

2. **DB 커넥션 점유 최소화**
   - 이미지 변환 같은 CPU 작업 중에는 트랜잭션 열지 않기
   - DB 업데이트가 필요할 때만 짧은 트랜잭션 사용

3. **실패 허용 설계**
   - 비동기 작업 실패 시 로그 + 재시도 가능한 구조
   - 원본은 이미 저장되어 있으므로 파생 파일은 나중에 재생성 가능

### 5.2 @Modifying 쿼리 사용 시 주의점

```java
// ❌ 잘못된 사용 - 영속성 컨텍스트 불일치 발생
@Modifying
@Query("UPDATE ...")
void updateSomething();

// ✅ 올바른 사용
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("UPDATE ...")
void updateSomething();
```

- `flushAutomatically = true`: UPDATE 전에 영속성 컨텍스트의 변경사항 DB 반영
- `clearAutomatically = true`: UPDATE 후 영속성 컨텍스트 캐시 비우기

### 5.3 쿼리 최적화 원칙

1. **엔티티 대신 ID 사용**
   ```java
   // ❌ 프록시 초기화 발생 가능
   findByMemberAndGuide(Member member, Guide guide);

   // ✅ ID만 사용하여 직접 쿼리
   existsByMemberIdAndGuideId(Long memberId, Long guideId);
   ```

2. **불필요한 SELECT 제거**
   ```java
   // ❌ 존재 확인 후 다시 조회
   if (repository.existsById(id)) {
       Entity entity = repository.findById(id);
   }

   // ✅ 한 번에 처리
   Optional<Entity> entity = repository.findById(id);
   ```

3. **Spring Data JPA의 deleteBy 주의**
   ```java
   // ❌ SELECT + DELETE (2쿼리)
   void deleteByMemberIdAndGuideId(Long memberId, Long guideId);

   // ✅ DELETE만 (1쿼리)
   @Modifying
   @Query("DELETE FROM GuideLike gl WHERE gl.member.id = :memberId AND gl.guide.id = :guideId")
   void deleteByMemberIdAndGuideId(@Param("memberId") Long memberId, @Param("guideId") Long guideId);
   ```

---

## 참고 자료

- [Spring @Async 공식 문서](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)
- [Spring Data JPA @Modifying](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.modifying-queries)
- [HikariCP 커넥션 풀 튜닝](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing)
- [k6 부하 테스트 가이드](https://grafana.com/docs/k6/latest/)
