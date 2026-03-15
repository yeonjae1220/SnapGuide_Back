package yeonjae.snapguide.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import yeonjae.snapguide.exception.CustomException;
import yeonjae.snapguide.exception.ErrorCode;
import yeonjae.snapguide.security.authentication.jwt.JwtToken;
import yeonjae.snapguide.security.authentication.jwt.JwtTokenProvider;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtTokenProvider")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    // 테스트용 비밀키 (256비트 이상, Base64 인코딩)
    private static final String TEST_SECRET = Base64.getEncoder()
            .encodeToString("test-secret-key-that-is-at-least-32-bytes-long!!".getBytes());

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "expAccess", 1800000L);   // 30분
        ReflectionTestUtils.setField(jwtTokenProvider, "expRefresh", 604800000L); // 7일
    }

    @Nested
    @DisplayName("generateToken 메서드")
    class GenerateToken {

        @Test
        @DisplayName("유효한 권한 정보로 토큰을 생성하면 accessToken과 refreshToken을 반환한다")
        void shouldGenerateBothTokensForValidAuthority() {
            // Arrange
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_MEMBER"));

            // Act
            JwtToken token = jwtTokenProvider.generateToken(authorities, "user@example.com");

            // Assert
            assertThat(token).isNotNull();
            assertThat(token.getAccessToken()).isNotBlank();
            assertThat(token.getRefreshToken()).isNotBlank();
            assertThat(token.getGrantType()).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("복수 권한으로 토큰을 생성하면 콤마 구분자로 인코딩된다")
        void shouldEncodeMultipleAuthoritiesWithComma() {
            // Arrange
            var authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_MEMBER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN")
            );

            // Act
            JwtToken token = jwtTokenProvider.generateToken(authorities, "admin@example.com");
            Authentication auth = jwtTokenProvider.getAuthentication(token.getAccessToken());

            // Assert
            assertThat(auth.getAuthorities())
                    .extracting(a -> a.getAuthority())
                    .containsExactlyInAnyOrder("ROLE_MEMBER", "ROLE_ADMIN");
        }
    }

    @Nested
    @DisplayName("createAccessToken 메서드")
    class CreateAccessToken {

        @Test
        @DisplayName("accessToken만 생성되고 refreshToken은 null이다")
        void shouldCreateAccessTokenOnlyWithoutRefreshToken() {
            // Arrange
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_MEMBER"));

            // Act
            JwtToken token = jwtTokenProvider.createAccessToken(authorities, "user@example.com");

            // Assert
            assertThat(token.getAccessToken()).isNotBlank();
            assertThat(token.getRefreshToken()).isNull();
        }
    }

    @Nested
    @DisplayName("validateToken 메서드")
    class ValidateToken {

        @Test
        @DisplayName("유효한 토큰은 true를 반환한다")
        void shouldReturnTrueForValidToken() {
            // Arrange
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_MEMBER"));
            JwtToken token = jwtTokenProvider.generateToken(authorities, "user@example.com");

            // Act & Assert
            assertThat(jwtTokenProvider.validateToken(token.getAccessToken())).isTrue();
        }

        @Test
        @DisplayName("만료된 토큰은 EXPIRED_TOKEN 예외를 던진다")
        void shouldThrowExpiredTokenExceptionForExpiredToken() {
            // Arrange - 만료 시간을 -1ms로 설정하여 즉시 만료
            ReflectionTestUtils.setField(jwtTokenProvider, "expAccess", -1L);
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_MEMBER"));
            JwtToken expiredToken = jwtTokenProvider.generateToken(authorities, "user@example.com");

            // Act & Assert
            assertThatThrownBy(() -> jwtTokenProvider.validateToken(expiredToken.getAccessToken()))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.EXPIRED_TOKEN));
        }

        @Test
        @DisplayName("위조된 토큰은 INVALID_SIGNATURE 예외를 던진다")
        void shouldThrowInvalidSignatureForTamperedToken() {
            // Arrange
            String tamperedToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.invalid_signature";

            // Act & Assert
            assertThatThrownBy(() -> jwtTokenProvider.validateToken(tamperedToken))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isIn(ErrorCode.INVALID_SIGNATURE, ErrorCode.INVALID_TOKEN));
        }

        @Test
        @DisplayName("빈 문자열 토큰은 예외를 던진다")
        void shouldThrowExceptionForBlankToken() {
            assertThatThrownBy(() -> jwtTokenProvider.validateToken(""))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("getAuthentication 메서드")
    class GetAuthentication {

        @Test
        @DisplayName("유효한 토큰에서 Authentication 객체를 올바르게 생성한다")
        void shouldExtractAuthenticationFromValidToken() {
            // Arrange
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_MEMBER"));
            JwtToken token = jwtTokenProvider.generateToken(authorities, "user@example.com");

            // Act
            Authentication auth = jwtTokenProvider.getAuthentication(token.getAccessToken());

            // Assert
            assertThat(auth).isNotNull();
            assertThat(auth.getName()).isEqualTo("user@example.com");
            assertThat(auth.getAuthorities())
                    .extracting(a -> a.getAuthority())
                    .containsExactly("ROLE_MEMBER");
        }

        @Test
        @DisplayName("권한 클레임이 없는 토큰은 INVALID_TOKEN 예외를 던진다")
        void shouldThrowInvalidTokenWhenNoAuthorityClaim() {
            // Arrange - 권한 없이 토큰 생성 (직접 Jwts 사용)
            SecretKey secretKey = Keys.hmacShaKeyFor(
                    java.util.Base64.getDecoder().decode(TEST_SECRET));
            String tokenWithoutAuth = Jwts.builder()
                    .subject("user@example.com")
                    .expiration(new java.util.Date(System.currentTimeMillis() + 1800000L))
                    .signWith(secretKey)
                    .compact();

            // Act & Assert
            assertThatThrownBy(() -> jwtTokenProvider.getAuthentication(tokenWithoutAuth))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_TOKEN));
        }
    }

    @Nested
    @DisplayName("parseExpiredToken 메서드")
    class ParseExpiredToken {

        @Test
        @DisplayName("만료된 토큰에서도 Claims를 추출할 수 있다")
        void shouldExtractClaimsFromExpiredToken() {
            // Arrange - 즉시 만료 토큰 생성
            ReflectionTestUtils.setField(jwtTokenProvider, "expAccess", -1L);
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_MEMBER"));
            JwtToken expiredToken = jwtTokenProvider.generateToken(authorities, "expired@example.com");

            // Act
            io.jsonwebtoken.Claims claims = jwtTokenProvider.parseExpiredToken(expiredToken.getAccessToken());

            // Assert
            assertThat(claims).isNotNull();
            assertThat(claims.getSubject()).isEqualTo("expired@example.com");
        }
    }

    @Nested
    @DisplayName("getExpiration 메서드")
    class GetExpiration {

        @Test
        @DisplayName("유효한 토큰의 남은 만료 시간을 양수로 반환한다")
        void shouldReturnPositiveExpirationForValidToken() {
            // Arrange
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_MEMBER"));
            JwtToken token = jwtTokenProvider.generateToken(authorities, "user@example.com");

            // Act
            long expiration = jwtTokenProvider.getExpiration(token.getAccessToken());

            // Assert
            assertThat(expiration).isPositive();
        }
    }
}
