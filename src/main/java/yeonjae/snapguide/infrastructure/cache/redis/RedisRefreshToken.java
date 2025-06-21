package yeonjae.snapguide.infrastructure.cache.redis;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import yeonjae.snapguide.security.authentication.jwt.RefreshToken;

@RedisHash(value = "MemberToken", timeToLive = 60 * 60 * 24 * 7) // 리프레시토큰과 expiretime 일치
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class RedisRefreshToken {
    @Id
    private String key; // email

    private String value; // refreshToken

    public RedisRefreshToken updateValue(String token) {
        this.value = token;
        return this;
    }
}
