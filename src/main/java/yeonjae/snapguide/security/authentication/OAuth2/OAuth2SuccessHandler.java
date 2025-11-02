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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final MemberRepository memberRepository;
    private final RedisRefreshTokenRepository redisRefreshTokenRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        CustomOauth2UserDetails principal = (CustomOauth2UserDetails) authentication.getPrincipal();
        Member oauthMember = principal.getMember();
        log.info("oauth login member info : {} ", oauthMember);

        // 토큰 생성
        JwtToken jwtToken = jwtTokenProvider.generateToken(oauthMember.getAuthority(), oauthMember.getEmail()); // HACK : 매개 변수 확인 필요
        String accessToken = jwtToken.getAccessToken();
        String refreshToken = jwtToken.getRefreshToken();

        // RefreshToken 저장
        RedisRefreshToken redisRefreshToken = RedisRefreshToken.builder()
                .key(authentication.getName())
                .value(refreshToken)
                .build();
        redisRefreshTokenRepository.save(redisRefreshToken);


        // 프론트에 리다이렉트로 토큰 전달 - 예시: URL 파라미터로 전달 (더 나은 방법은 헤더 or 쿠키) TODO : sendRedirect 말고 JSON 응답이나 쿠키로 변경 필요
        String redirectUrl = UriComponentsBuilder
                .fromUriString("http://localhost:8080/index.html")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);

        // JSON 형태로 응답
//        response.setContentType("application/json");
//        response.setCharacterEncoding("UTF-8");
//
//        Map<String, String> tokenResponse = new HashMap<>();
//        tokenResponse.put("accessToken", accessToken);
//        tokenResponse.put("refreshToken", refreshToken);
//
//        response.getWriter().write(objectMapper.writeValueAsString(tokenResponse));

    }
}
