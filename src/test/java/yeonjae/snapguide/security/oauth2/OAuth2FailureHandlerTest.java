package yeonjae.snapguide.security.oauth2;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.util.ReflectionTestUtils;
import yeonjae.snapguide.security.authentication.OAuth2.HttpCookieOAuth2AuthorizationRequestRepository;
import yeonjae.snapguide.security.authentication.OAuth2.OAuth2FailureHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("OAuth2FailureHandler")
@ExtendWith(MockitoExtension.class)
class OAuth2FailureHandlerTest {

    @Mock
    private HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    private OAuth2FailureHandler failureHandler;

    private static final String FRONTEND_URL = "http://localhost:3000/login";
    private static final String APP_REDIRECT_URI = "snapguide://auth/callback";

    @BeforeEach
    void setUp() {
        failureHandler = new OAuth2FailureHandler(authorizationRequestRepository);
        ReflectionTestUtils.setField(failureHandler, "frontendRedirectUrl", FRONTEND_URL);
        ReflectionTestUtils.setField(failureHandler, "appRedirectUri", APP_REDIRECT_URI);
    }

    private AuthenticationException mockAuthException(String message) {
        AuthenticationException exception = mock(AuthenticationException.class);
        doReturn(message).when(exception).getLocalizedMessage();
        return exception;
    }

    @Nested
    @DisplayName("오픈 리다이렉트 방지")
    class OpenRedirectPrevention {

        @Test
        @DisplayName("허용되지 않은 redirect_uri 쿠키가 있으면 기본 frontendRedirectUrl로 리다이렉트한다")
        void shouldRedirectToDefaultWhenMaliciousRedirectUriInCookie() throws Exception {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            request.setCookies(new Cookie(
                    HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME,
                    "https://malicious-site.com/steal"
            ));
            AuthenticationException exception = mockAuthException("OAuth2 error");

            // Act
            failureHandler.onAuthenticationFailure(request, response, exception);

            // Assert
            assertThat(response.getRedirectedUrl())
                    .startsWith(FRONTEND_URL)
                    .doesNotContain("malicious-site.com");
        }

        @Test
        @DisplayName("허용된 frontendRedirectUrl이 쿠키에 있으면 해당 URL로 리다이렉트한다")
        void shouldRedirectToFrontendUrlWhenAllowed() throws Exception {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            request.setCookies(new Cookie(
                    HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME,
                    FRONTEND_URL
            ));
            AuthenticationException exception = mockAuthException("OAuth2 error");

            // Act
            failureHandler.onAuthenticationFailure(request, response, exception);

            // Assert
            assertThat(response.getRedirectedUrl())
                    .startsWith(FRONTEND_URL)
                    .contains("error=authentication_failed");
        }

        @Test
        @DisplayName("redirect_uri 쿠키가 없으면 기본 frontendRedirectUrl로 리다이렉트한다")
        void shouldRedirectToFrontendUrlWhenNoCookie() throws Exception {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            AuthenticationException exception = mockAuthException("OAuth2 error");

            // Act
            failureHandler.onAuthenticationFailure(request, response, exception);

            // Assert
            assertThat(response.getRedirectedUrl()).startsWith(FRONTEND_URL);
        }
    }

    @Nested
    @DisplayName("제네릭 에러 메시지")
    class GenericErrorMessage {

        @Test
        @DisplayName("에러 파라미터가 authentication_failed 고정값으로 전달된다 (실제 예외 메시지 미노출)")
        void shouldUseGenericErrorCodeNotActualMessage() throws Exception {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            AuthenticationException exception = mockAuthException("Sensitive internal error details");

            // Act
            failureHandler.onAuthenticationFailure(request, response, exception);

            // Assert
            assertThat(response.getRedirectedUrl())
                    .contains("error=authentication_failed")
                    .doesNotContain("Sensitive internal error details");
        }
    }

    @Nested
    @DisplayName("임시 쿠키 정리")
    class CookieCleanup {

        @Test
        @DisplayName("인증 실패 시 OAuth2 요청 쿠키를 삭제한다")
        void shouldRemoveAuthorizationRequestCookiesOnFailure() throws Exception {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            AuthenticationException exception = mockAuthException("OAuth2 error");

            // Act
            failureHandler.onAuthenticationFailure(request, response, exception);

            // Assert
            verify(authorizationRequestRepository).removeAuthorizationRequestCookies(request, response);
        }
    }
}
