package yeonjae.snapguide.security.authentication.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import yeonjae.snapguide.exception.CustomException;
import yeonjae.snapguide.exception.ErrorCode;

import java.io.IOException;
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    private final JwtTokenProvider jwtTokenProvider;
    private final RequestMatcher whiteListMatcher;

    private final String UTF_8 = "utf-8";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)  throws ServletException, IOException {
        if (!whiteListMatcher.matches(request)) {
            // 화이트리스트 요청은 필터 생략
            filterChain.doFilter(request, response);
            return;
        }

        try {
            log.info("🔍 auth 헤더: {}", request.getHeader("auth"));
            // 1. Request Header 로부터 Access Token을 추출한다.
            String token = jwtTokenProvider.resolveToken(request);
            log.info("token :" + token);
            // 2. 추출한 Token의 유효성 검증 및 사용자 정보 파싱
            if (token != null && jwtTokenProvider.validateToken(token)) {
                // Token이 유효할 경우, Authentication 객체를 생성하여 SecurityContext에 저장한다.
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                // 4. SecurityContext에 인증 정보 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            // 5. 다음 필터로 진행
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            log.warn("JWT 토큰이 만료되었습니다.");
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("유효하지 않은 JWT 토큰입니다.");
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        } catch (UsernameNotFoundException e) {
            log.warn("유저를 찾을 수 없습니다.");
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        } catch (Exception e) {
            log.error("인증 필터에서 예외 발생", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}

//    @Bean
//    public JwtAuthenticationFilter jwtAuthenticationFilter() {
//        final RequestMatcher matcher = new WhiteListRequestMatcher(SecurityConstants.AuthenticationWhiteList.getAllPatterns());
//        final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(matcher);
//        filter.setAuthenticationFailureHandler(new AuthenticationEntryPointFailureHandler(this.authenticationEntryPoint));
//        filter.setAuthenticationManager(new ProviderManager(this.authenticationProvider));
//        return filter;
//    }