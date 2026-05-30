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
