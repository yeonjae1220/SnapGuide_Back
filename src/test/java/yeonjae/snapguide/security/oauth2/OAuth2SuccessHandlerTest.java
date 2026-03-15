package yeonjae.snapguide.security.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.repository.OAuth2AuthorizationCodeRepository;
import yeonjae.snapguide.repository.RedisRefreshTokenRepository;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;
import yeonjae.snapguide.security.authentication.OAuth2.CustomOauth2UserDetails;
import yeonjae.snapguide.security.authentication.OAuth2.HttpCookieOAuth2AuthorizationRequestRepository;
import yeonjae.snapguide.security.authentication.OAuth2.OAuth2AuthorizationCode;
import yeonjae.snapguide.security.authentication.OAuth2.OAuth2SuccessHandler;
import yeonjae.snapguide.security.authentication.jwt.JwtTokenProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@DisplayName("OAuth2SuccessHandler")
@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private ObjectMapper objectMapper;
    @Mock private MemberRepository memberRepository;
    @Mock private RedisRefreshTokenRepository redisRefreshTokenRepository;
    @Mock private OAuth2AuthorizationCodeRepository authCodeRepository;
    @Mock private Authentication authentication;
    @Mock private CustomOauth2UserDetails principal;

    @InjectMocks
    private OAuth2SuccessHandler successHandler;

    private static final String FRONTEND_URL = "http://localhost:3000/auth/callback";
    private static final String APP_REDIRECT_URI = "snapguide://auth/callback";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(successHandler, "frontendRedirectUrl", FRONTEND_URL);
        ReflectionTestUtils.setField(successHandler, "appRedirectUri", APP_REDIRECT_URI);

        Member member = Member.builder().id(1L).email("user@example.com").nickname("testUser").build();
        given(authentication.getPrincipal()).willReturn(principal);
        given(principal.getMember()).willReturn(member);
        given(authCodeRepository.save(any(OAuth2AuthorizationCode.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("오픈 리다이렉트 방지")
    class OpenRedirectPrevention {

        @Test
        @DisplayName("허용되지 않은 redirect_uri 쿠키가 있으면 기본 appRedirectUri로 리다이렉트한다")
        void shouldFallbackToDefaultWhenMaliciousRedirectUri() throws Exception {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            request.setCookies(new Cookie(
                    HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME,
                    "https://attacker.com/steal-token"
            ));

            // Act
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // Assert
            assertThat(response.getRedirectedUrl())
                    .startsWith(APP_REDIRECT_URI)
                    .doesNotContain("attacker.com")
                    .contains("code=");
        }

        @Test
        @DisplayName("허용된 frontendRedirectUrl 쿠키가 있으면 해당 URL로 리다이렉트한다")
        void shouldRedirectToFrontendUrlWhenAllowed() throws Exception {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            request.setCookies(new Cookie(
                    HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME,
                    FRONTEND_URL
            ));

            // Act
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // Assert
            assertThat(response.getRedirectedUrl())
                    .startsWith(FRONTEND_URL)
                    .contains("code=");
        }

        @Test
        @DisplayName("redirect_uri 쿠키가 없으면 기본 appRedirectUri로 리다이렉트한다")
        void shouldUseDefaultAppRedirectUriWhenNoCookie() throws Exception {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // Act
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // Assert
            assertThat(response.getRedirectedUrl())
                    .startsWith(APP_REDIRECT_URI)
                    .contains("code=");
        }
    }

    @Nested
    @DisplayName("Authorization Code Flow")
    class AuthorizationCodeFlow {

        @Test
        @DisplayName("인증 성공 시 일회용 authorization code를 Redis에 저장한다")
        void shouldSaveAuthorizationCodeToRepository() throws Exception {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // Act
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // Assert
            verify(authCodeRepository).save(any(OAuth2AuthorizationCode.class));
        }

        @Test
        @DisplayName("리다이렉트 URL에 code 파라미터가 포함된다")
        void shouldIncludeCodeParameterInRedirectUrl() throws Exception {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // Act
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // Assert
            assertThat(response.getRedirectedUrl()).contains("code=");
        }
    }
}
