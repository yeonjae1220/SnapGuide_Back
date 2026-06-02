package yeonjae.snapguide.infrastructure.cache.redis.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import yeonjae.snapguide.controller.guideController.guideDto.GuideResponseDto;
import yeonjae.snapguide.controller.guideController.guideDto.RegionClusterDto;

import java.time.Duration;
import java.util.List;

/**
 * Spring Cache Abstraction을 위한 Redis 캐시 설정
 *
 * RedisConfig vs RedisCacheConfig:
 * - RedisConfig: RedisTemplate을 통한 수동 캐싱 (Token, Session 등)
 * - RedisCacheConfig: @Cacheable 어노테이션 기반 자동 캐싱 (조회 결과 등)
 *
 * <h3>직렬화 전략 — 캐시별 타입 바인딩</h3>
 * 과거 {@code Jackson2JsonRedisSerializer<Object>(Object.class)}는 타입 정보 없이
 * 저장해, 캐시 히트 시 DTO가 {@code LinkedHashMap}으로 역직렬화되었다. 컨트롤러
 * 반환 타입이 record(예: {@link RegionClusterDto})인 경우 MVC가 record 접근자를
 * LinkedHashMap에 호출하다 ClassCastException → 500을 유발했다.
 *
 * <p>해결: 각 캐시에 <b>구체 제네릭 타입을 바인딩한 전용 직렬화기</b>를 사용한다.
 * 타입을 알고 역직렬화하므로 record/일반 DTO 모두 정확히 복원되며 {@code @class}
 * 메타데이터에 의존하지 않아 기존 캐시 엔트리와도 호환된다(flush 불필요).
 * {@code GenericJackson2JsonRedisSerializer}는 final 타입(record)에 타입 태그를
 * 붙이지 않아 이 문제를 해결하지 못하므로 사용하지 않는다.
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    /** @Cacheable 메서드 결과를 캐시별 타입에 맞춰 직렬화하는 CacheManager */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 캐시별 구체 타입 직렬화기
        RedisSerializer<Object> nearbySerializer =
                typedSerializer(objectMapper, List.class, GuideResponseDto.class);
        RedisSerializer<Object> aggregateSerializer =
                typedSerializer(objectMapper, List.class, RegionClusterDto.class);

        // 명시 설정이 없는 캐시용 기본 직렬화기 (@class 포함, 비-record DTO에 안전)
        RedisSerializer<Object> defaultSerializer =
                new GenericJackson2JsonRedisSerializer();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(baseConfig(defaultSerializer))
                .withCacheConfiguration("nearbyGuides", baseConfig(nearbySerializer))
                .withCacheConfiguration("regionAggregate", baseConfig(aggregateSerializer))
                .build();
    }

    /** 공통 캐시 설정(TTL·null 비캐싱·키 직렬화)에 값 직렬화기만 주입 */
    private RedisCacheConfiguration baseConfig(RedisSerializer<Object> valueSerializer) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(CACHE_TTL)
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer));
    }

    /** {@code List<E>} 등 구체 제네릭 타입에 바인딩된 Jackson 직렬화기 생성 */
    private RedisSerializer<Object> typedSerializer(
            ObjectMapper objectMapper, Class<?> collectionType, Class<?> elementType) {
        JavaType javaType = objectMapper.getTypeFactory()
                .constructParametricType(collectionType, elementType);
        return new Jackson2JsonRedisSerializer<>(objectMapper, javaType);
    }
}
