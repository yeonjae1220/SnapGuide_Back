package yeonjae.snapguide.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ACCESS_PREFIX = "blacklist:access:";
    private static final String REFRESH_PREFIX = "blacklist:refresh:";

    /**
     * access 토큰 블랙리스트 처리
     */
    public void blacklistAccessToken(String token, long expirationMillis) {
        log.info("Access token blacklisted");
        redisTemplate.opsForValue().set(
                ACCESS_PREFIX + token,
                "blacklisted",
                expirationMillis,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * refresh 토큰 블랙리스트 처리
     */
    public void blacklistRefreshToken(String token, long expirationMillis) {
        log.info("Refresh token blacklisted");
        redisTemplate.opsForValue().set(
                REFRESH_PREFIX + token,
                "blacklisted",
                expirationMillis,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * access 토큰이 블랙리스트인지 확인
     */
    public boolean isAccessTokenBlacklisted(String token) {
//        return Boolean.TRUE.equals(redisTemplate.hasKey(ACCESS_PREFIX + token)); // NPE(NullPointerException)를 방지하면서 안전하게 boolean 비교를 수행합니다. null인 경우도 false 처리됩니다.
        String key = ACCESS_PREFIX + token;
        Boolean hasKey = redisTemplate.hasKey(key);

        if (Boolean.TRUE.equals(hasKey)) {
            String blacklistedToken = (String) redisTemplate.opsForValue().get(key);
            log.warn("Blacklisted token access attempt detected");
        } else {
            log.info("✅ 블랙리스트에 없음");
        }

        return Boolean.TRUE.equals(hasKey);
    }

    /**
     * refresh 토큰이 블랙리스트인지 확인
     */
    public boolean isRefreshTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(REFRESH_PREFIX + token));
    }
}
