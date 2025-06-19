package yeonjae.snapguide.security.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
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
import yeonjae.snapguide.domain.member.Authority;
import yeonjae.snapguide.security.authentication.exception.JwtAccessDeniedHandler;
import yeonjae.snapguide.security.authentication.exception.JwtAuthenticationEntryPoint;
import yeonjae.snapguide.security.authentication.jwt.JwtAuthenticationFilter;
import yeonjae.snapguide.security.authentication.jwt.JwtTokenProvider;
import yeonjae.snapguide.security.constant.SecurityConstants;
import yeonjae.snapguide.security.matcher.WhiteListRequestMatcher;

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

//    private final UserDetailsService userDetailsService;
//    private final PasswordEncoder passwordEncoder;


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


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                // 조건 별 요청 허용 or 제한 설정
                .authorizeHttpRequests(
                        authorize -> authorize
                                .requestMatchers(SecurityConstants.AuthenticationWhiteList.TEST_API).hasAnyAuthority("MEMBER", "ADMIN")
                                .requestMatchers(SecurityConstants.AuthenticationWhiteList.SWAGGER_V3).permitAll()
                                .requestMatchers(SecurityConstants.AuthenticationWhiteList.AUTH_API).permitAll()
                                .requestMatchers(SecurityConstants.AuthenticationWhiteList.USER_API).permitAll()
                                .requestMatchers(SecurityConstants.AuthenticationWhiteList.LOCAL_LOGIN_API).permitAll()
                                .anyRequest()
                                .authenticated()
                )
                .cors(AbstractHttpConfigurer::disable)  // CORS 설정 (또는 cors -> cors.disable())
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

        final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, matcher);

        // 선택: 인증 실패 시 동작 처리
//        filter.setAuthenticationFailureHandler(
//                new AuthenticationEntryPointFailureHandler(jwtAuthenticationEntryPoint)
//        );

        // 선택: 별도 ProviderManager가 필요할 경우
        // filter.setAuthenticationManager(new ProviderManager(authenticationProvider));

        return filter;
    }

}

