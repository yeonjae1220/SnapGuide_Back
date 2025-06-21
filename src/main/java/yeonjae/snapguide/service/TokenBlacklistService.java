package yeonjae.snapguide.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ACCESS_PREFIX = "blacklist:access:";
    private static final String REFRESH_PREFIX = "blacklist:refresh:";

    /**
     * access 토큰 블랙리스트 처리
     */
    public void blacklistAccessToken(String token, long expirationMillis) {
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
        return Boolean.TRUE.equals(redisTemplate.hasKey(ACCESS_PREFIX + token));
    }

    /**
     * refresh 토큰이 블랙리스트인지 확인
     */
    public boolean isRefreshTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(REFRESH_PREFIX + token));
    }
}
