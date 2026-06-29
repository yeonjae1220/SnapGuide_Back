package yeonjae.snapguide.security.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;
import yeonjae.snapguide.security.authentication.OAuth2.CustomOAuth2AuthorizationRequestResolver;
import yeonjae.snapguide.security.authentication.OAuth2.HttpCookieOAuth2AuthorizationRequestRepository;
import yeonjae.snapguide.security.authentication.OAuth2.OAuth2FailureHandler;
import yeonjae.snapguide.security.authentication.OAuth2.OAuth2SuccessHandler;
import yeonjae.snapguide.security.AccessLogFilter;
import yeonjae.snapguide.security.authentication.exception.JwtAccessDeniedHandler;
import yeonjae.snapguide.security.authentication.exception.JwtAuthenticationEntryPoint;
import yeonjae.snapguide.security.authentication.jwt.JwtAuthenticationFilter;
import yeonjae.snapguide.security.authentication.jwt.JwtTokenProvider;
import yeonjae.snapguide.security.ClientIpResolver;
import yeonjae.snapguide.security.adminlogin.AdminAuthenticationFailureHandler;
import yeonjae.snapguide.security.adminlogin.AdminAuthenticationSuccessHandler;
import yeonjae.snapguide.security.adminlogin.AdminLoginAttemptFilter;
import yeonjae.snapguide.security.adminlogin.AdminLoginAttemptService;
import yeonjae.snapguide.security.constant.SecurityConstants;
import yeonjae.snapguide.security.matcher.WhiteListRequestMatcher;
import yeonjae.snapguide.service.CustomOauth2UserService;
import yeonjae.snapguide.service.CustomUserDetailsService;
import yeonjae.snapguide.service.TokenBlacklistService;
import yeonjae.snapguide.security.internal.ServiceTokenAuthFilter;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final TokenBlacklistService tokenBlacklistService;

    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    private final Environment environment;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
    private final CustomOauth2UserService customOauth2UserService;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final ClientIpResolver clientIpResolver;
    private final AdminLoginAttemptService adminLoginAttemptService;

    @Value("${admin.email}")
    private String adminEmail;

    /** BCrypt hash — k8s Secret ADMIN_PASSWORD_BCRYPT 에서 주입 */
    @Value("${admin.password.bcrypt}")
    private String adminPasswordBcrypt;

    /** 콘솔 집계용 서비스 토큰 — 미설정 시 /api/internal/** 전부 차단(fail-closed) */
    @Value("${console.internal-token:}")
    private String consoleInternalToken;

    private static final java.util.Set<String> KNOWN_TEST_HASHES = java.util.Set.of(
            "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi."
    );

    @PostConstruct
    public void validateAdminCredentials() {
        boolean isTest = java.util.Arrays.asList(environment.getActiveProfiles()).contains("test");
        if (isTest) {
            log.info("[Admin] test 프로파일 — admin 자격증명 검증 생략");
            return;
        }
        if (adminEmail == null || adminEmail.isBlank()) {
            throw new IllegalStateException("[Admin] ADMIN_EMAIL 환경변수가 설정되지 않았습니다.");
        }
        if (adminEmail.endsWith(".local") || adminEmail.endsWith(".example.com")) {
            throw new IllegalStateException(
                    "[Admin] ADMIN_EMAIL 기본값이 운영 환경에서 사용되었습니다. ADMIN_EMAIL을 실제 값으로 설정하세요.");
        }
        if (adminPasswordBcrypt == null
                || adminPasswordBcrypt.contains("placeholder")
                || adminPasswordBcrypt.length() < 60
                || (!adminPasswordBcrypt.startsWith("$2a$")
                    && !adminPasswordBcrypt.startsWith("$2b$")
                    && !adminPasswordBcrypt.startsWith("$2y$"))) {
            throw new IllegalStateException(
                    "[Admin] ADMIN_PASSWORD_BCRYPT가 유효한 BCrypt 해시가 아닙니다.");
        }
        if (KNOWN_TEST_HASHES.contains(adminPasswordBcrypt)) {
            throw new IllegalStateException(
                    "[Admin] ADMIN_PASSWORD_BCRYPT가 공개된 테스트 해시입니다. 운영 환경에서 사용할 수 없습니다.");
        }
        log.info("[Admin] admin 계정 설정이 유효합니다: email={}", adminEmail);
    }

    /** local/test 프로파일에서만 true — docker(운영)에서는 Swagger 차단 */
    private boolean isDevProfile() {
        for (String profile : environment.getActiveProfiles()) {
            if (profile.equalsIgnoreCase("local") || profile.equalsIgnoreCase("test")) return true;
        }
        return environment.getActiveProfiles().length == 0;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService adminUserDetailsService() {
        var admin = User.builder()
                .username(adminEmail)
                .password(adminPasswordBcrypt)
                .authorities("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    /** Admin SSR 전용 AuthenticationManager (InMemory — DB 독립적) */
    @Bean
    public AuthenticationManager adminAuthenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(adminUserDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    /**
     * API(JWT) 로그인 전용 AuthenticationManager.
     * @Primary: AuthService가 @Qualifier 없이 주입 가능.
     */
    @Bean
    @Primary
    public AuthenticationManager apiAuthenticationManager(
            CustomUserDetailsService customUserDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    /**
     * Admin 전용 Security chain (세션 기반).
     * /admin/** 경로만 담당. formLogin + httpOnly 세션 쿠키로 인증.
     * JWT 필터 미적용 — XSS 토큰 탈취 위협과 완전히 분리.
     */
    /**
     * 콘솔 집계 전용 Security chain (서비스 토큰 기반, 읽기 전용).
     * /api/internal/** 만 담당. ServiceTokenAuthFilter가 X-Internal-Token을
     * 상수시간 비교로 검증 — 통과해야만 컨트롤러 도달. JWT/세션 필터 미적용.
     */
    @Bean
    @Order(0)
    public SecurityFilterChain internalConsoleFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/internal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(new ServiceTokenAuthFilter(consoleInternalToken),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/admin/**")
                .authenticationManager(adminAuthenticationManager())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/login").permitAll()
                        .anyRequest().hasAuthority("ADMIN")
                )
                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        // 무차별 대입 방지: 실패/성공을 IP·계정 차원으로 카운트
                        .successHandler(new AdminAuthenticationSuccessHandler(
                                adminLoginAttemptService, clientIpResolver))
                        .failureHandler(new AdminAuthenticationFailureHandler(
                                adminLoginAttemptService, clientIpResolver))
                        .permitAll()
                )
                // lockout 강제: 비밀번호 검증 전에 429 + Retry-After 로 차단
                .addFilterBefore(new AdminLoginAttemptFilter(adminLoginAttemptService, clientIpResolver),
                        UsernamePasswordAuthenticationFilter.class)
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/admin/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation().changeSessionId()
                        .maximumSessions(1)
                )
                .csrf(Customizer.withDefaults())
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; frame-ancestors 'none'"))
                        .frameOptions(f -> f.deny())
                )
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityConstants.AuthenticationWhiteList.TEST_API)
                            .access((a, ctx) -> new org.springframework.security.authorization.AuthorizationDecision(isDevProfile()))
                        .requestMatchers(SecurityConstants.AuthenticationWhiteList.GUIDE_PUBLIC_API).permitAll()
                        .requestMatchers(SecurityConstants.AuthenticationWhiteList.MEDIA_FILES).permitAll()
                        // Swagger: local/test 프로파일에서만 허용
                        .requestMatchers(SecurityConstants.AuthenticationWhiteList.SWAGGER_V3)
                            .access((a, ctx) -> new org.springframework.security.authorization.AuthorizationDecision(isDevProfile()))
                        .requestMatchers(SecurityConstants.AuthenticationWhiteList.AUTH_API).permitAll()
                        .requestMatchers(SecurityConstants.AuthenticationWhiteList.USER_API).permitAll()
                        .requestMatchers(SecurityConstants.AuthenticationWhiteList.LOCAL_LOGIN_API).permitAll()
                        .requestMatchers(SecurityConstants.AuthenticationWhiteList.OAUTH_API).permitAll()
                        .requestMatchers(SecurityConstants.AuthenticationWhiteList.DEV_TOOL).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").hasAuthority("ADMIN")
                        .requestMatchers(SecurityConstants.AuthenticationWhiteList.LOCATION_API).permitAll()
                        .requestMatchers(SecurityConstants.AuthenticationWhiteList.PWA_PUBLIC).permitAll()
                        .requestMatchers(SecurityConstants.AuthenticationWhiteList.MAPS_PUBLIC).permitAll()
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(config -> config
                                .baseUri("/oauth2/authorization")
                                .authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository)
                                .authorizationRequestResolver(new CustomOAuth2AuthorizationRequestResolver(
                                        clientRegistrationRepository, "/oauth2/authorization"))
                        )
                        .redirectionEndpoint(config -> config.baseUri("/login/oauth2/code/*"))
                        .userInfoEndpoint(config -> config.userService(customOauth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )
                .headers(headers -> headers
                        .contentTypeOptions(c -> {})
                        .frameOptions(f -> f.deny())
                        .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000).includeSubDomains(true))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                "script-src 'self' https://maps.googleapis.com https://maps.gstatic.com; " +
                                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                                "font-src 'self' https://fonts.gstatic.com; " +
                                "img-src 'self' data: https:; " +
                                "connect-src 'self' https://maps.googleapis.com https://maps.gstatic.com; " +
                                "worker-src 'self' blob:"))
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new AccessLogFilter(), JwtAuthenticationFilter.class)
                .build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        final RequestMatcher matcher =
                new WhiteListRequestMatcher(SecurityConstants.AuthenticationWhiteList.getAllPatterns());
        return new JwtAuthenticationFilter(jwtTokenProvider, matcher, tokenBlacklistService);
    }

    /**
     * JwtAuthenticationFilter 서블릿 자동 등록 방지.
     * /admin/** 요청에 JWT 필터가 끼어들지 않도록 한다.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }
}
