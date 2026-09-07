package yeonjae.snapguide.security.authentication.OAuth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import com.nimbusds.oauth2.sdk.util.StringUtils;
import yeonjae.snapguide.infrastructure.cookie.CookieUtil;


@Component
@Slf4j
public class HttpCookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    private static final int COOKIE_EXPIRE_SECONDS = 180;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        log.info("---- [Cookie Load] Trying to load authorization request from cookie.");
        return CookieUtil.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(cookie -> {
                    // 🔴 substring(0, 10) 은 쿠키가 10자 미만이면 StringIndexOutOfBoundsException 을 던진다.
                    //    이 람다는 인증 진입 경로라, 공격자가 짧은 oauth2_auth_request 쿠키를 심는 것만으로
                    //    로그인 전체를 500 으로 막을 수 있었다. 로그는 부수 표면이므로 본 작업을 절대
                    //    막지 않아야 한다 (GLOBAL-PIT-135). 값 자체(state·PKCE 포함)도 남기지 않는다.
                    log.info("---- [Cookie Load] Found 'oauth2_auth_request' cookie ({} chars).", cookie.getValue().length());
                    return CookieUtil.deserialize(cookie, OAuth2AuthorizationRequest.class);
                })
                .orElseGet(() -> {
                    log.warn("---- [Cookie Load] !!! 'oauth2_auth_request' cookie NOT FOUND !!!");
                    return null;
                });
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            CookieUtil.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            CookieUtil.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
            return;
        }

        CookieUtil.addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, CookieUtil.serialize(authorizationRequest), COOKIE_EXPIRE_SECONDS);
        String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
        log.info("---- [Cookie Save] Saving authorization request. Client-provided redirect_uri: {}", redirectUriAfterLogin);
        if (StringUtils.isNotBlank(redirectUriAfterLogin)) {
            CookieUtil.addCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUriAfterLogin, COOKIE_EXPIRE_SECONDS);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        return this.loadAuthorizationRequest(request);
    }

    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        CookieUtil.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
    }
}
