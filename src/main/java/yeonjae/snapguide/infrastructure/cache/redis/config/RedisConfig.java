package yeonjae.snapguide.infrastructure.cache.redis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableRedisRepositories // Redis 저장소 기능 활성화
public class RedisConfig {
    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Lettuce vs Jedis ⇒ Lettuce 선택, Lettuce 라이브러리를 사용해서 Redis에 연결
        // Lettuce는 Jedis보다 성능이 좋고 비동기 처리가 가능함
        // return new LettuceConnectionFactory(this.host, this.port);
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(this.host);
        config.setPort(this.port);
        return new LettuceConnectionFactory(config);
    }

    // RedisTemplate 설정
    // RedisTemplate은 DB 서버에 Set, Get, Delete 등을 사용할 수 있음
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        // Redis와 통신할 때 사용할 템플릿 설정
        final RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        this.setKeyValueSerializer(redisTemplate);
        this.setHashKeyValueSerializer(redisTemplate);
//        this.setDefaultSerializer(redisTemplate);
        return redisTemplate;
    }

//    private void setDefaultSerializer(RedisTemplate<String, Object> redisTemplate) {
//        redisTemplate.setDefaultSerializer(new StringRedisSerializer());
//    }

    // hash key, hash value에 대한 직렬화 방법 설정
    private void setHashKeyValueSerializer(RedisTemplate<String, Object> redisTemplate) {
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());
    }
    // key, value에 대한 직렬화 방법 설정
    private void setKeyValueSerializer(RedisTemplate<String, Object> redisTemplate) {
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
    }
}
