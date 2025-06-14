package yeonjae.snapguide.security.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import yeonjae.snapguide.security.authentication.jwt.JwtAuthenticationFilter;
import yeonjae.snapguide.security.constant.SecurityConstants;
import yeonjae.snapguide.security.matcher.WhiteListRequestMatcher;

@Configuration
@EnableWebSecurity  // 스프링 시큐리티 필터가 스프링 필터체인에 등록이 된다.
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .authorizeHttpRequests(
                        authorize -> authorize
                                .requestMatchers(SecurityConstants.AuthenticationWhiteList.TEST_API).permitAll()
                                .requestMatchers(SecurityConstants.AuthenticationWhiteList.SWAGGER_V3).permitAll()
                                .requestMatchers(SecurityConstants.AuthenticationWhiteList.AUTH_API).permitAll()
                                .requestMatchers(SecurityConstants.AuthenticationWhiteList.USER_API).permitAll()
                                .requestMatchers(SecurityConstants.AuthenticationWhiteList.LOCAL_LOGIN_API).permitAll()
                                .anyRequest()
                                .authenticated()
                )
                .cors(AbstractHttpConfigurer::disable)  // CORS 설정 (또는 cors -> cors.disable())
                .csrf(AbstractHttpConfigurer::disable)  // CSRF 비활성화
                .httpBasic(AbstractHttpConfigurer::disable) // HTTP Basic 비활성화
                .formLogin(AbstractHttpConfigurer::disable) // Form Login 비활성화
                // 세션 사용 안함 (JWT 기반 인증 등 stateless 보안 구조일 경우)
                // + 토큰에 저장된 유저정보를 활용하여야 하기 때문에 CustomUserDetailService 클래스를 생성합니다.
                .sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // .exceptionHandling(configurer -> configurer.accessDeniedHandler(this.accessDeniedHandler))

                // JWT 등 커스텀 필터가 있다면 여기에 추가
                // JwtAuthenticationFilter를 UsernamePasswordAuthenticationFilter 전에 넣는다
                 .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        final RequestMatcher matcher = new WhiteListRequestMatcher(SecurityConstants.AuthenticationWhiteList.getAllPatterns());
        final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(matcher);
        filter.setAuthenticationFailureHandler(new AuthenticationEntryPointFailureHandler(this.authenticationEntryPoint));
        filter.setAuthenticationManager(new ProviderManager(this.authenticationProvider));
        return filter;
    }

}

