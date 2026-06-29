package yeonjae.snapguide.security.authentication.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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
import yeonjae.snapguide.exception.ErrorResponse;
import yeonjae.snapguide.service.TokenBlacklistService;

import java.io.IOException;
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    private final JwtTokenProvider jwtTokenProvider;
    private final RequestMatcher whiteListMatcher;
    private final TokenBlacklistService tokenBlacklistService;

    private final String UTF_8 = "utf-8";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        log.debug("[JwtAuthenticationFilter] {} {}", request.getMethod(), request.getRequestURI());

        try {
            if (!whiteListMatcher.matches(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = jwtTokenProvider.resolveToken(request);
            if (token != null && jwtTokenProvider.validateToken(token)) {
                if (!tokenBlacklistService.isAccessTokenBlacklisted(token)) {
                    log.info("토큰은 블랙리스트에 없음 (정상)");
                    Authentication authentication = jwtTokenProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    MDC.put("userId", authentication.getName());
                } else {
                    log.warn("블랙리스트에 있는 토큰입니다. 요청 차단");
                    setErrorResponse(response, ErrorCode.INVALID_TOKEN);
                    return;
                }
            }

            filterChain.doFilter(request, response);

        } catch (CustomException e) {
            setErrorResponse(response, e.getErrorCode());
        } catch (ExpiredJwtException e) {
            log.warn("JWT 토큰이 만료되었습니다.");
            setErrorResponse(response, ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("유효하지 않은 JWT 토큰입니다.");
            setErrorResponse(response, ErrorCode.INVALID_TOKEN);
        } catch (UsernameNotFoundException e) {
            log.warn("유저를 찾을 수 없습니다.");
            setErrorResponse(response, ErrorCode.USER_NOT_FOUND);
        } catch (Exception e) {
            log.error("인증 필터에서 예외 발생", e);
            setErrorResponse(response, ErrorCode.INTERNAL_SERVER_ERROR);
        } finally {
            MDC.remove("userId");
        }
    }

    // 응답을 처리하는 헬퍼 메서드 추가
    private void setErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(errorCode.getStatus().value()); // ErrorCode enum에 정의된 HTTP 상태 코드 사용
        log.info("[setErrorResponse] status={}", errorCode.getStatus().value());

        // ObjectMapper를 사용하여 ErrorResponse 객체를 JSON 문자열로 변환
        ObjectMapper objectMapper = new ObjectMapper();
        // Java 8 날짜/시간 타입(LocalDateTime 등) 지원을 위한 모듈 등록
        objectMapper.findAndRegisterModules();

        // ErrorResponse는 직접 만드셔야 하는 DTO 클래스입니다. (예: status, code, message 필드 포함)
        String jsonResponse = objectMapper.writeValueAsString(
                new ErrorResponse(errorCode)
        );

        response.getWriter().write(jsonResponse);
    }
}
