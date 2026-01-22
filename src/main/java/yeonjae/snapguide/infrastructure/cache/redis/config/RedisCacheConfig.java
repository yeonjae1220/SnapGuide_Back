package yeonjae.snapguide.infrastructure.cache.redis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
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
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Spring Cache Abstraction을 위한 Redis 캐시 설정
 *
 * RedisConfig vs RedisCacheConfig:
 * - RedisConfig: RedisTemplate을 통한 수동 캐싱 (Token, Session 등)
 * - RedisCacheConfig: @Cacheable 어노테이션 기반 자동 캐싱 (조회 결과 등)
 *
 * 이 설정을 통해:
 * - @Cacheable: 메서드 결과를 자동으로 캐싱
 * - @CacheEvict: 캐시 무효화
 * - @CachePut: 캐시 업데이트
 * 어노테이션을 사용할 수 있습니다.
 */
@Configuration
@EnableCaching // Spring Cache 기능 활성화 (AOP 기반 캐싱)
public class RedisCacheConfig {

    /**
     * CacheManager Bean 생성
     *
     * CacheManager는 Spring이 캐시를 관리하는 핵심 컴포넌트입니다.
     * @Cacheable 어노테이션이 붙은 메서드를 AOP로 가로채서
     * 캐시 확인 → 저장 → 반환을 자동으로 처리합니다.
     *
     * @param connectionFactory RedisConfig에서 생성된 RedisConnectionFactory 주입
     * @return RedisCacheManager
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // ObjectMapper 설정: 타입 정보 없이 단순 JSON으로 직렬화 (역직렬화 문제 해결)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // LocalDateTime 등 지원
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // ISO-8601 형식

        // Jackson2JsonRedisSerializer: 타입 정보 없는 단순 JSON 직렬화
        // - 복잡한 DTO 구조도 안정적으로 처리
        // - 타입 정보(@class) 없이 순수 JSON으로 저장
        Jackson2JsonRedisSerializer<Object> serializer =
                new Jackson2JsonRedisSerializer<>(Object.class);
        serializer.setObjectMapper(objectMapper);

        // Redis 캐시 기본 설정
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                // TTL 설정: 30분 후 자동 삭제
                // 가이드 데이터는 자주 변경되지 않으므로 30분이 적절
                // (부하 테스트 고려: 5분 테스트도 캐시 만료 없이 완료)
                .entryTtl(Duration.ofMinutes(30))

                // null 값은 캐싱하지 않음
                // (빈 결과를 계속 캐싱하면 메모리 낭비 + 실제 데이터 생성 시 반영 안됨)
                .disableCachingNullValues()

                // Key 직렬화: String → UTF-8 바이트 배열
                // 예: "nearbyGuides::37.557:126.924:2.0"
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()
                        )
                )

                // Value 직렬화: Java 객체 → JSON
                // 예: List<GuideResponseDto> → JSON 배열
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer)
                );

        // RedisCacheManager 생성 및 반환
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig) // 기본 설정 적용
                .build();
    }
}
