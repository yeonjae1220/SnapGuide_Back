package yeonjae.snapguide.security.authentication.OAuth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.infrastructure.cache.redis.RedisRefreshToken;
import yeonjae.snapguide.repository.RedisRefreshTokenRepository;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;
import yeonjae.snapguide.security.authentication.jwt.JwtToken;
import yeonjae.snapguide.security.authentication.jwt.JwtTokenProvider;
import yeonjae.snapguide.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import yeonjae.snapguide.repository.OAuth2AuthorizationCodeRepository;
import yeonjae.snapguide.infrastructure.cookie.CookieUtil;
import com.nimbusds.oauth2.sdk.util.StringUtils;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final MemberRepository memberRepository;
    private final RedisRefreshTokenRepository redisRefreshTokenRepository;
    private final OAuth2AuthorizationCodeRepository authCodeRepository;

    @Value("${spring.myapp.frontend-redirect-url}")
    private String frontendRedirectUrl;

    @Value("${spring.myapp.app-redirect-uri}")
    private String appRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        CustomOauth2UserDetails principal = (CustomOauth2UserDetails) authentication.getPrincipal();
        Member oauthMember = principal.getMember();
        log.info("oauth login member info : {} ", oauthMember);

        // 쿠키에서 클라이언트가 보낸 redirect_uri 가져오기
        String targetUrl = CookieUtil.getCookie(request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(cookie -> cookie.getValue())
                .orElse(appRedirectUri); // 없으면 모바일 앱 URI를 기본값으로 사용

        // 오픈 리다이렉트 방지: 허용된 URI만 사용
        Set<String> allowedUris = Set.of(frontendRedirectUrl, appRedirectUri);
        if (!allowedUris.contains(targetUrl)) {
            log.warn("허용되지 않은 redirect_uri 시도: {}", targetUrl);
            targetUrl = appRedirectUri;
        }
        log.info("🎯 Target redirect URL: {}", targetUrl);

        // 🔒 모든 클라이언트에 대해 Authorization Code 방식 적용
        handleAuthorizationCodeFlow(response, oauthMember, targetUrl);
    }

    /**
     * Authorization Code Flow 처리 (모바일 앱 + 웹 통합)
     * - 일회용 코드를 생성하여 Redis에 저장
     * - 클라이언트가 보낸 redirect_uri로 code만 리다이렉트
     * - 보안 강화: 토큰은 URL에 노출하지 않음
     */
    private void handleAuthorizationCodeFlow(HttpServletResponse response, Member oauthMember, String targetUrl) throws IOException {
        // 1. 일회용 authorization code 생성 (UUID)
        String authCode = UUID.randomUUID().toString();

        // 2. Redis에 code와 사용자 이메일 매핑 저장 (5분 TTL)
        OAuth2AuthorizationCode codeEntity = OAuth2AuthorizationCode.of(authCode, oauthMember.getEmail());
        authCodeRepository.save(codeEntity);

        log.info("🔐 Authorization code 생성: {} for user: {}, redirect to: {}", authCode, oauthMember.getEmail(), targetUrl);

        // 3. 클라이언트가 보낸 redirect_uri에 code 추가하여 리다이렉트
        String redirectUrl = UriComponentsBuilder
                .fromUriString(targetUrl)
                .queryParam("code", authCode)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
