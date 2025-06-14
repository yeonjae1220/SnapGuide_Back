package yeonjae.snapguide.security.authentication.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public JwtAuthenticationFilter(RequestMatcher matcher) {
        super(matcher);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        return null;
    }

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final String UTF_8 = "utf-8";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. Request Header 로부터 Access Token을 추출한다.
            String token = jwtTokenProvider.resolveToken((HttpServletRequest) request);

            // 2. 추출한 Token의 유효성 검사를 진행한다.
            if (token != null && jwtTokenProvider.validateToken(token)) {
                // Token이 유효할 경우, Authentication 객체를 생성하여 SecurityContext에 저장한다.
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        } catch (TokenException e) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setCharacterEncoding(UTF_8);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            response.getWriter().write(
                    objectMapper.writeValueAsString(
                            ResponseDto.create(e.getMessage())
                    )
            );
        }
    }
}