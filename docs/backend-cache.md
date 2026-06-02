# SnapGuide Backend — Redis 캐시 설계

> 마지막 업데이트: 2026-06-02
> 대상: `src/main/java/.../infrastructure/cache/redis/`

---

## 개요

Spring Cache Abstraction(`@Cacheable` / `@CacheEvict`) 기반. Redis를 저장소로 사용.

| 설정 클래스 | 역할 |
|---|---|
| `RedisConfig` | `RedisTemplate` 수동 캐싱 (Token, Session 등) |
| `RedisCacheConfig` | `@Cacheable` 어노테이션 자동 캐싱 (조회 결과) |

---

## 캐시 목록

| 캐시 이름 | 타입 | 키 | TTL | 무효화 트리거 |
|---|---|---|---|---|
| `nearbyGuides` | `List<GuideResponseDto>` | `{lat}:{lng}:{radius}` | 30분 | 가이드 생성/수정/삭제, 좋아요 토글 |
| `regionAggregate` | `List<RegionClusterDto>` | `"COUNTRY"` / `"CONTINENT"` | 30분 | 가이드 생성/수정/삭제, 좋아요 토글 |

---

## 직렬화 전략 — 캐시별 타입 바인딩

### 문제 (2026-06-02 발견)

구 설정의 `Jackson2JsonRedisSerializer<Object>(Object.class)`는 타입 정보 없이 순수 JSON으로 저장.
캐시 히트 시 역직렬화가 `LinkedHashMap`으로 귀결되는데, 컨트롤러 반환 타입이 Java record
(`RegionClusterDto`)이면 MVC가 record 접근자를 `LinkedHashMap`에 호출 →
`ClassCastException` → **매 캐시 히트마다 500** 발생.

`nearbyGuides`는 키 공간(`lat:lng:radius`)이 방대해 캐시 히트가 드물어 잠복.
`regionAggregate`는 키가 `"COUNTRY"` 고정이라 첫 호출 이후 항상 히트 → 항상 터짐.

`GenericJackson2JsonRedisSerializer`는 **`final` 타입(record)에 `@class` 태그를 붙이지 않아**
이 문제를 해결하지 못한다.

### 해결

캐시별로 **구체 제네릭 타입에 바인딩된 전용 직렬화기** 적용:

```java
// RedisCacheConfig.java
JavaType javaType = objectMapper.getTypeFactory()
    .constructParametricType(List.class, GuideResponseDto.class);
return new Jackson2JsonRedisSerializer<>(objectMapper, javaType);
```

타입을 알고 역직렬화하므로 record/일반 DTO 모두 정확히 복원.
`@class` 메타데이터에 의존하지 않아 **기존 캐시 엔트리와 포맷 호환 → Redis flush 불필요**.

### 신규 캐시 추가 시

```java
// RedisCacheConfig.cacheManager()에 추가
RedisSerializer<Object> mySerializer =
    typedSerializer(objectMapper, List.class, MyNewDto.class);

return RedisCacheManager.builder(connectionFactory)
    .cacheDefaults(baseConfig(defaultSerializer))
    .withCacheConfiguration("nearbyGuides", baseConfig(nearbySerializer))
    .withCacheConfiguration("regionAggregate", baseConfig(aggregateSerializer))
    .withCacheConfiguration("myNewCache", baseConfig(mySerializer))  // ← 추가
    .build();
```

---

## 무효화 어노테이션

가이드 CRUD와 좋아요 토글은 두 캐시를 함께 무효화:

```java
@Caching(evict = {
    @CacheEvict(value = "nearbyGuides", allEntries = true),
    @CacheEvict(value = "regionAggregate", allEntries = true)
})
public void deleteGuide(...) { ... }
```

---

## regionAggregate 설계 메모

- **전역 집계** (bbox 없음): 결과가 소규모(국가 수 ≤ 250, 대륙 ≤ 7)라 캐싱 효율 높음
- **키 공간**: level당 1개 키 → TTL 30분 + 가이드 변경 시 즉시 무효화
- **썸네일**: 그룹 내 `likeCount` 최대 가이드의 첫 번째 미디어 URL (1회 쿼리)
- **집계 로직**: Java 레벨 groupBy (DB 집계 쿼리 아님) → `GuideAggregateRow` 경량 프로젝션으로 전체 로드 후 서비스에서 처리
- **프론트엔드 dedup**: `lastAggLevelRef`로 동일 level 중복 API 호출 방지

---

## 주의사항

- `GenericJackson2JsonRedisSerializer`(기본 직렬화기)는 `@class` 메타데이터를 사용.
  명시 설정이 없는 캐시에 적용되며, record 타입을 캐싱하면 동일한 역직렬화 버그 발생 가능.
  **record를 반환하는 메서드에는 반드시 위 패턴으로 타입 전용 직렬화기를 등록할 것.**
- Redis는 k8s ClusterIP(`host: redis`) — 외부 미노출. 내부 신뢰 환경.
