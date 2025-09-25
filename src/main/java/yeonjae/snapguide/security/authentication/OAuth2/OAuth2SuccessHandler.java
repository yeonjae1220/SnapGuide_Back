package yeonjae.snapguide.security.authentication.OAuth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.infrastructure.cache.redis.RedisRefreshToken;
import yeonjae.snapguide.infrastructure.cookie.CookieUtil;
import yeonjae.snapguide.repository.RedisRefreshTokenRepository;
import yeonjae.snapguide.security.authentication.jwt.JwtToken;
import yeonjae.snapguide.security.authentication.jwt.JwtTokenProvider;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.util.Optional;

import static yeonjae.snapguide.security.authentication.OAuth2.HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisRefreshTokenRepository redisRefreshTokenRepository;
    // ✅ 1. 쿠키 기반의 인증 요청 저장소를 주입받습니다.
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    // ✅ 2. 웹 클라이언트를 위한 기본 Redirect URI를 설정 파일에서 주입받습니다.
    @Value("${spring.myapp.frontend-redirect-url}")
    private String webDefaultRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        CustomOauth2UserDetails principal = (CustomOauth2UserDetails) authentication.getPrincipal();
        Member oauthMember = principal.getMember();
        log.info("OAuth login successful for member: {}", oauthMember);

        // JWT 토큰 생성
        JwtToken jwtToken = jwtTokenProvider.generateToken(oauthMember.getAuthority(), oauthMember.getEmail());
        String accessToken = jwtToken.getAccessToken();
        String refreshToken = jwtToken.getRefreshToken();

        // RefreshToken을 Redis에 저장
        redisRefreshTokenRepository.save(new RedisRefreshToken(authentication.getName(), refreshToken));

        // ✅ 3. 쿠키에서 클라이언트가 요청했던 Redirect URI를 꺼냅니다.
        Optional<String> redirectUri = CookieUtil.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);

        // 쿠키에 redirect_uri가 없으면, 웹을 위한 기본 URI를 사용합니다.
        String targetUrl = redirectUri.orElse(webDefaultRedirectUri);

        // ✅ 4. 동적으로 얻은 targetUrl에 토큰을 파라미터로 추가합니다.
        String finalRedirectUrl = UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build()
                .toUriString();

        // ✅ 5. 인증 과정에서 사용된 임시 쿠키들을 삭제합니다.
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        getRedirectStrategy().sendRedirect(request, response, finalRedirectUrl);
    }
}
