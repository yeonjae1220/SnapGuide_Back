package yeonjae.snapguide.security.authentication.OAuth2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

/**
 * OAuth2 인증 후 일회용 authorization code를 저장하는 Redis 엔티티
 * - 모바일 앱에서 안전하게 토큰을 교환하기 위한 중간 코드
 * - 5분간 유효하며, 사용 후 즉시 삭제됨
 */
@Getter
@RedisHash("oauth2_auth_code")
@Builder
@AllArgsConstructor
public class OAuth2AuthorizationCode {

    @Id
    private String code;  // UUID로 생성된 일회용 코드

    private String email; // 사용자 이메일 (토큰 발급 시 식별용)

    @TimeToLive
    private Long expiration; // TTL (기본 300초 = 5분)

    public static OAuth2AuthorizationCode of(String code, String email) {
        return OAuth2AuthorizationCode.builder()
                .code(code)
                .email(email)
                .expiration(300L) // 5분
                .build();
    }
}
