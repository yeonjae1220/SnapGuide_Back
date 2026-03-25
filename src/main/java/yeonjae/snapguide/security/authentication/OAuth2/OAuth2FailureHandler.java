package yeonjae.snapguide.security.authentication.OAuth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import yeonjae.snapguide.infrastructure.cookie.CookieUtil;

import java.io.IOException;
import java.util.Set;

import static yeonjae.snapguide.security.authentication.OAuth2.HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME;

/**
 * OAuth2 로그인 실패 시 호출되는 핸들러.
 * 인증 과정에서 생성된 임시 쿠키를 삭제하고,
 * 클라이언트가 원래 요청했던 redirect_uri로 에러 메시지와 함께 리디렉션합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    /**
     * OAuth2 인증 요청 정보를 쿠키에 저장하는 저장소.
     * 실패 시 이 저장소에 저장된 쿠키를 삭제해야 합니다.
     */
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Value("${spring.myapp.frontend-redirect-url}")
    private String frontendRedirectUrl;

    @Value("${spring.myapp.app-redirect-uri}")
    private String appRedirectUri;

    /**
     * 인증 실패 시 Spring Security에 의해 호출되는 메소드.
     *
     * @param request   현재 HTTP 요청
     * @param response  현재 HTTP 응답
     * @param exception 발생한 인증 예외
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        // 1. 발생한 예외를 로그에 기록합니다.
        log.error("OAuth2 Login Failed: {}", exception.getLocalizedMessage());

        // 2. 클라이언트가 로그인 요청 시 전달했던 redirect_uri를 쿠키에서 가져옵니다.
        // 이 URI는 웹일 수도 있고, 앱의 커스텀 스킴일 수도 있습니다.
        String targetUrl = CookieUtil.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse(frontendRedirectUrl);

        // 오픈 리다이렉트 방지: 허용된 URI만 사용
        Set<String> allowedUris = Set.of(frontendRedirectUrl, appRedirectUri);
        if (!allowedUris.contains(targetUrl)) {
            log.warn("허용되지 않은 redirect_uri 시도: {}", targetUrl);
            targetUrl = frontendRedirectUrl;
        }

        // 예외 메시지 URL 노출 방지: 제네릭 에러 코드만 전달
        String finalRedirectUrl = UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("error", "authentication_failed")
                .build().toUriString();

        // 4. (가장 중요) 인증 과정에서 사용된 모든 임시 쿠키를 삭제합니다.
        // 이 작업을 하지 않으면, 다음 로그인 시도 시 남아있는 쿠키 때문에 'state' 불일치 오류가 발생합니다.
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        // 5. 최종적으로 생성된 URL로 사용자를 리디렉션시킵니다.
        getRedirectStrategy().sendRedirect(request, response, finalRedirectUrl);
    }
}

