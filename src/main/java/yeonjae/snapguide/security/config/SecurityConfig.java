package yeonjae.snapguide.security.config;

import lombok.RequiredArgsConstructor;
import org.apache.catalina.connector.Connector;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;
import yeonjae.snapguide.domain.member.Authority;
import yeonjae.snapguide.security.authentication.OAuth2.CustomOAuth2AuthorizationRequestResolver;
import yeonjae.snapguide.security.authentication.OAuth2.HttpCookieOAuth2AuthorizationRequestRepository;
import yeonjae.snapguide.security.authentication.OAuth2.OAuth2FailureHandler;
import yeonjae.snapguide.security.authentication.OAuth2.OAuth2SuccessHandler;
import yeonjae.snapguide.security.authentication.exception.JwtAccessDeniedHandler;
import yeonjae.snapguide.security.authentication.exception.JwtAuthenticationEntryPoint;
import yeonjae.snapguide.security.authentication.jwt.JwtAuthenticationFilter;
import yeonjae.snapguide.security.authentication.jwt.JwtTokenProvider;
import yeonjae.snapguide.security.constant.SecurityConstants;
import yeonjae.snapguide.security.matcher.WhiteListRequestMatcher;
import yeonjae.snapguide.service.CustomOauth2UserService;
import yeonjae.snapguide.service.CustomUserDetailsService;
import yeonjae.snapguide.service.TokenBlacklistService;

@Configuration
@EnableWebSecurity  // 스프링 시큐리티 필터가 스프링 필터체인에 등록이 된다.
@RequiredArgsConstructor
public class SecurityConfig {

    //    private final AuthenticationEntryPointImpl authenticationEntryPoint;
//    private final AccessDeniedHandlerImpl accessDeniedHandler;
//    private final JwtAuthenticationProvider authenticationProvider;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final TokenBlacklistService tokenBlacklistService;

    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    private final CorsConfigurationSource corsConfigurationSource;

//    private final CustomUserDetailsService userDetailsService;
//    private final PasswordEncoder passwordEncoder;

    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
    private final CustomOauth2UserService customOauth2UserService;
    private final ClientRegistrationRepository clientRegistrationRepository;


    // 이렇게 하면 userDetailsService와 passwordEncoder를 사용하여 내부적으로 인증 처리가 구성

    /**
     * 	1.	http.getSharedObject(AuthenticationManagerBuilder.class):
     * 	•	HttpSecurity에서 인증 구성에 필요한 AuthenticationManagerBuilder 객체를 가져옵니다.
     * 	2.	builder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder):
     * 	•	사용자 인증 시 어떤 방식으로 사용자 정보를 로드할지(UserDetailsService)
     * 	•	비밀번호를 어떻게 검증할지(PasswordEncoder) 설정합니다.
     * 	3.	builder.build():
     * 	•	설정한 내용을 바탕으로 AuthenticationManager 인스턴스를 생성합니다.
     */


//    @Bean
//    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
//        AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
//        builder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
//        return builder.build();
//    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Admin 전용 Security chain (세션 기반).
     * /admin/** 경로만 담당하며 formLogin + httpOnly 세션 쿠키로 인증한다.
     * JWT 필터를 걸지 않아 accessToken XSS 탈취 위협과 완전히 분리된다.
     */
    @Bean
    @org.springframework.core.annotation.Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/admin/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/login").permitAll()
                        .anyRequest().hasAuthority("ADMIN")
                )
                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        .defaultSuccessUrl("/admin/dashboard", true)
                        .failureUrl("/admin/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/admin/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                )
                .csrf(Customizer.withDefaults()) // CSRF 활성화 (Thymeleaf가 토큰 자동 삽입)
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; frame-ancestors 'none'"))
                        .frameOptions(f -> f.deny())
                )
                .build();
    }

    @Bean
    @org.springframework.core.annotation.Order(2)
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                // 조건 별 요청 허용 or 제한 설정
                .authorizeHttpRequests(
                        authorize -> authorize
                                .requestMatchers(SecurityConstants.AuthenticationWhiteList.TEST_API).permitAll()
                                .requestMatchers(SecurityConstants.AuthenticationWhiteList.GUIDE_PUBLIC_API).permitAll() // 비인증 공개 가이드 조회
                                .requestMatchers(SecurityConstants.AuthenticationWhiteList.SWAGGER_V3).permitAll()
                                .requestMatchers(SecurityConstants.AuthenticationWhiteList.AUTH_API).permitAll()
                                .requestMatchers(SecurityConstants.AuthenticationWhiteList.USER_API).permitAll()
                                .requestMatchers(SecurityConstants.AuthenticationWhiteList.LOCAL_LOGIN_API).permitAll()
                                .requestMatchers(SecurityConstants.AuthenticationWhiteList.OAUTH_API).permitAll()
                                .requestMatchers(SecurityConstants.AuthenticationWhiteList.DEV_TOOL).permitAll()
                                .requestMatchers(SecurityConstants.AuthenticationWhiteList.FILE_IO).permitAll() // 로컬 파일 저장 url 열어둠
                                .requestMatchers(SecurityConstants.AuthenticationWhiteList.ACTUATOR).permitAll() // 모니터링 메트릭
                                .requestMatchers(SecurityConstants.AuthenticationWhiteList.LOCATION_API).permitAll() // 위치 검색 (비인증 허용)
                                .requestMatchers(SecurityConstants.AuthenticationWhiteList.PWA_PUBLIC).permitAll() // PWA 공개 리소스
                                .requestMatchers("/api/admin/**").hasAuthority("ADMIN") // 어드민 전용
                                .anyRequest()
                                .authenticated()
                )
                // OAuth 2.0 로그인 방식 설정
//                .oauth2Login((auth) -> auth.loginPage("/oauth-login/login")
////                        .defaultSuccessUrl("/oauth-login")
////                        .failureUrl("/oauth-login/login")
//                        .successHandler(oAuth2SuccessHandler)
//                        .failureHandler(oAuth2FailureHandler)
//                        .permitAll())
//                .logout((auth) -> auth
//                        .logoutUrl("/oauth-login/logout")
//                        .logoutSuccessUrl("/oauth-login/login"))
                // OAuth2.0 세션에서 토큰 방식으로 변경
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(config -> config
                                .baseUri("/oauth2/authorization")
                                .authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository)
                                .authorizationRequestResolver(new CustomOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization"))
                        )
                        .redirectionEndpoint(config -> config
                                .baseUri("/login/oauth2/code/*")
                        )
                        .userInfoEndpoint(config -> config
                                .userService(customOauth2UserService)
                        )
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )
                // .cors(AbstractHttpConfigurer::disable)  // CORS 설정 (또는 cors -> cors.disable())
                .headers(headers -> headers
                        .contentTypeOptions(c -> {})
                        .frameOptions(f -> f.deny())
                        .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000).includeSubDomains(true))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                "script-src 'self' 'unsafe-inline' https://maps.googleapis.com https://maps.gstatic.com https://static.cloudflareinsights.com; " +
                                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                                "font-src 'self' https://fonts.gstatic.com; " +
                                "img-src 'self' data: https:; " +
                                "connect-src 'self' https://maps.googleapis.com https://maps.gstatic.com; " +
                                "worker-src 'self' blob:"))
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)  // CSRF 비활성화 // Cookie 기반 인증이 아닌, JWT 기반 인증이기에 csrf 사용 X
                .httpBasic(AbstractHttpConfigurer::disable) // HTTP Basic 비활성화 // ID, password 문자열을 Base64로 인코딩하여 전달하는 구조
                .formLogin(AbstractHttpConfigurer::disable) // Form Login 비활성화
                .logout(AbstractHttpConfigurer::disable)
                // 세션 사용 안함 (JWT 기반 인증 등 stateless 보안 구조일 경우) // Spring Security Session 정책 -> Session 생성 및 사용 X
                // + 토큰에 저장된 유저정보를 활용하여야 하기 때문에 CustomUserDetailService 클래스를 생성합니다.
                .sessionManagement(configurer -> configurer
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .exceptionHandling(configurer -> configurer
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(this.jwtAccessDeniedHandler))

                // JWT 등 커스텀 필터가 있다면 여기에 추가
                // JwtAuthenticationFilter를 UsernamePasswordAuthenticationFilter 전에 넣는다
//                 .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                // JwtFilter 를 addFilterBefore 로 등록했던 JwtSecurityConfig 클래스를 적용
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        final RequestMatcher matcher =
                new WhiteListRequestMatcher(SecurityConstants.AuthenticationWhiteList.getAllPatterns());

        return new JwtAuthenticationFilter(jwtTokenProvider, matcher, tokenBlacklistService);
    }

    /**
     * JwtAuthenticationFilter는 @Bean이므로 Spring Boot가 서블릿 필터로 자동 등록한다.
     * 자동 등록을 막아 Security FilterChain 안에서만 동작하도록 한다.
     * 이렇게 하지 않으면 /admin/** 요청에도 JWT 필터가 먼저 실행되어 admin 체인이 무력화된다.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    // HTTPS 커넥터 설정 - 로컬 개발 환경에서는 비활성화
    // 프로덕션 환경에서 리버스 프록시 뒤에서 실행할 때만 필요
//    @Profile("docker")
//    @Bean
//    public ServletWebServerFactory servletContainer() {
//        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
//
//        // 이 설정은 리버스 프록시(HTTP)로부터 온 요청도 HTTPS에서 온 것처럼 처리하도록 합니다.
//        tomcat.addAdditionalTomcatConnectors(createSslConnector());
//        return tomcat;
//    }
//
//    private Connector createSslConnector() {
//        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
//        // 이 커넥터는 8080 포트로 들어오는 HTTP 요청을 처리합니다.
//        connector.setPort(8080);
//
//        // 하지만 Spring Boot에게 이 요청이 원래는 HTTPS였다고 알려줍니다.
//        connector.setScheme("https");
//        connector.setSecure(true);
//        return connector;
//    }

    // 서버 로그가 지저분해지는 것을 막기 위해 아래 경로에 대한 요청 무시
//    @Bean
//    public WebSecurityCustomizer webSecurityCustomizer() {
//        // 정적 리소스들을 Security 필터에서 제외
//        return (web) -> web.ignoring()
//                .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
//                .requestMatchers(
//                        "/favicon.ico",
//                        "/.well-known/**"
//                );
//    }

}

