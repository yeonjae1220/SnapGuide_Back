package yeonjae.snapguide.security.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import yeonjae.snapguide.exception.CustomException;
import yeonjae.snapguide.exception.ErrorCode;

import java.util.List;

/**
 * Redis Lua 스크립트 기반 슬라이딩 카운터 Rate Limiter.
 * INCR + EXPIRE 를 단일 원자적 Lua 스크립트로 실행 — 크래시 시 TTL 누락 방지.
 */
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    /**
     * INCR 후 키가 새로 생성(count == 1)될 때만 EXPIRE 설정 (원자적 실행).
     */
    private static final RedisScript<Long> INCREMENT_SCRIPT = RedisScript.of(
            "local c = redis.call('INCR', KEYS[1])\n" +
            "if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end\n" +
            "return c",
            Long.class);

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${snapguide.rate-limit.login-limit:10}")
    private int loginLimit;

    @Value("${snapguide.rate-limit.signup-limit:5}")
    private int signupLimit;

    @Value("${snapguide.rate-limit.window-seconds:600}")
    private int windowSeconds;

    @Value("${snapguide.rate-limit.location-limit:20}")
    private int locationLimit;

    @Value("${snapguide.rate-limit.aggregate-limit:30}")
    private int aggregateLimit;

    @Value("${snapguide.rate-limit.nearby-limit:60}")
    private int nearbyLimit;

    @Value("${snapguide.rate-limit.map-key-limit:60}")
    private int mapKeyLimit;

    /**
     * 로그인 rate limit 검사.
     * IP 기준 10회/10분, 이메일 기준 30회/10분.
     */
    public void checkLoginRate(String clientIp, String email) {
        check("rl:login:ip:" + clientIp, loginLimit);
        check("rl:login:acc:" + email, loginLimit * 3);
    }

    /**
     * 회원가입 rate limit 검사.
     * IP 기준 5회/10분.
     */
    public void checkSignupRate(String clientIp) {
        check("rl:signup:ip:" + clientIp, signupLimit);
    }

    /**
     * IP 지오로케이션 rate limit 검사.
     * IP 기준 20회/10분 — 외부 서비스(ipapi.co) 과금 및 DoS 방지.
     */
    public void checkLocationRate(String clientIp) {
        check("rl:location:ip:" + clientIp, locationLimit);
    }

    /**
     * 국가/대륙 집계 rate limit 검사.
     * IP 기준 30회/10분 — 캐시 미스 시 PostGIS full scan 유발 가능성 차단.
     */
    public void checkAggregateRate(String clientIp) {
        check("rl:aggregate:ip:" + clientIp, aggregateLimit);
    }

    /**
     * 주변 가이드 조회 rate limit 검사.
     * IP 기준 60회/10분 — 공개 지도 탐색에서 좌표를 바꿔 캐시를 우회하는 남용을 제한한다.
     */
    public void checkNearbyRate(String clientIp) {
        check("rl:nearby:ip:" + clientIp, nearbyLimit);
    }

    /**
     * Maps JavaScript API 키 조회 rate limit 검사.
     * 키 자체는 브라우저 공개 값이지만, 반복 스크래핑과 로그 잡음을 제한한다.
     */
    public void checkMapKeyRate(String clientIp) {
        check("rl:maps:key:ip:" + clientIp, mapKeyLimit);
    }

    private void check(String key, int limit) {
        Long count = redisTemplate.execute(
                INCREMENT_SCRIPT,
                List.of(key),
                String.valueOf(windowSeconds));
        if (count != null && count > limit) {
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }
}
