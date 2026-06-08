package yeonjae.snapguide.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import yeonjae.snapguide.domain.member.dto.MemberRequestDto;
import yeonjae.snapguide.domain.member.dto.MemberResponseDto;
import yeonjae.snapguide.security.ClientIpResolver;
import yeonjae.snapguide.security.authentication.jwt.JwtToken;
import yeonjae.snapguide.security.authentication.jwt.TokenRequestDto;
import yeonjae.snapguide.security.config.SecurityConfig;
import yeonjae.snapguide.security.ratelimit.RateLimiterService;
import yeonjae.snapguide.service.AuthService;
import yeonjae.snapguide.service.GoogleOAuthService;
import yeonjae.snapguide.service.memberSerivce.MemberService;

import jakarta.servlet.http.Cookie;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 단위 테스트
 * 회원가입, 로그인, 토큰 재발급, 로그아웃, 회원탈퇴, OAuth 기능 테스트
 */
@WebMvcTest(controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private MemberService memberService;

    @MockBean
    private RedisTemplate<String, String> redisTemplate;

    @MockBean
    private GoogleOAuthService googleOAuthService;

    @MockBean
    private RateLimiterService rateLimiterService; // RateLimiterService Mock — Redis 없이 테스트 가능

    @MockBean
    private ClientIpResolver clientIpResolver; // @Component이지만 @WebMvcTest 슬라이스에 자동 포함되지 않아 Mock 필요

    private MemberRequestDto signupRequest;
    private JwtToken mockJwtToken;

    @BeforeEach
    void setUp() {
        signupRequest = new MemberRequestDto();
        signupRequest.setEmail("test@example.com");
        signupRequest.setPassword("Password123");
        signupRequest.setNickname("testUser");

        mockJwtToken = JwtToken.builder()
                .grantType("Bearer")
                .accessToken("mock-access-token")
                .refreshToken("mock-refresh-token")
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/signup - 회원가입 성공")
    void signup_Success() throws Exception {
        // given
        MemberResponseDto mockResponse = MemberResponseDto.builder()
                .email("test@example.com")
                .build();

        given(authService.signup(any(MemberRequestDto.class)))
                .willReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(authService, times(1)).signup(any(MemberRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/auth/login - 로그인 성공")
    void login_Success() throws Exception {
        // given
        MemberRequestDto loginRequest = new MemberRequestDto();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("Password123");

        given(authService.login(any(MemberRequestDto.class)))
                .willReturn(mockJwtToken);

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grantType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());

        verify(authService, times(1)).login(any(MemberRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/auth/reissue - 토큰 재발급 성공")
    void reissue_Success() throws Exception {
        // given
        JwtToken newToken = JwtToken.builder()
                .grantType("Bearer")
                .accessToken("new-access-token")
                .refreshToken(null)
                .build();

        given(authService.reissue(any(TokenRequestDto.class)))
                .willReturn(newToken);

        // when & then
        mockMvc.perform(post("/api/auth/reissue")
                        .with(csrf())
                        .cookie(new Cookie("refresh_token", "valid-refresh-token")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist());

        verify(authService, times(1)).reissue(any(TokenRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/auth/logout - 로그아웃 성공")
    void logout_Success() throws Exception {
        // given
        given(authService.logout(any(TokenRequestDto.class)))
                .willReturn("test@example.com");

        Map<String, String> body = Map.of("accessToken", "valid-access-token");

        // when & then
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .cookie(new Cookie("refresh_token", "valid-refresh-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("로그아웃 되었습니다."));

        verify(authService, times(1)).logout(any(TokenRequestDto.class));
    }

    @Test
    @DisplayName("DELETE /api/auth/delete - 회원탈퇴 성공")
    void deleteMember_Success() throws Exception {
        // given
        String email = "test@example.com";
        given(authService.logout(any(TokenRequestDto.class)))
                .willReturn(email);

        doNothing().when(memberService).deleteMember(email);

        Map<String, String> body = Map.of("accessToken", "valid-access-token");

        // when & then
        mockMvc.perform(delete("/api/auth/delete")
                        .with(csrf())
                        .cookie(new Cookie("refresh_token", "valid-refresh-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("탈퇴 처리 되었습니다."));

        verify(authService, times(1)).logout(any(TokenRequestDto.class));
        verify(memberService, times(1)).deleteMember(email);
    }

    @Test
    @DisplayName("GET /api/auth/test - 인증된 사용자 확인")
    @WithMockUser(username = "test@example.com")
    void authTest_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/auth/test"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("인증된 사용자: test@example.com"));
    }

    @Test
    @DisplayName("POST /api/auth/google/token - Google OAuth 토큰 교환 성공")
    void exchangeGoogleAuthCode_Success() throws Exception {
        // given
        Map<String, String> request = new HashMap<>();
        request.put("code", "google-auth-code-12345");

        given(googleOAuthService.exchangeCodeForToken("google-auth-code-12345"))
                .willReturn(mockJwtToken);

        // when & then
        mockMvc.perform(post("/api/auth/google/token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grantType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());

        verify(googleOAuthService, times(1)).exchangeCodeForToken("google-auth-code-12345");
    }

    @Test
    @DisplayName("POST /api/auth/google/token - code가 없는 경우 400 에러")
    void exchangeGoogleAuthCode_EmptyCode_BadRequest() throws Exception {
        // given
        Map<String, String> request = new HashMap<>();
        request.put("code", "");

        // when & then
        mockMvc.perform(post("/api/auth/google/token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Authorization code is required"));

        verify(googleOAuthService, never()).exchangeCodeForToken(anyString());
    }

    @Test
    @DisplayName("POST /api/auth/google/token - code가 null인 경우 400 에러")
    void exchangeGoogleAuthCode_NullCode_BadRequest() throws Exception {
        // given
        Map<String, String> request = new HashMap<>();

        // when & then
        mockMvc.perform(post("/api/auth/google/token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Authorization code is required"));

        verify(googleOAuthService, never()).exchangeCodeForToken(anyString());
    }

    @Test
    @DisplayName("POST /api/auth/signup - 이메일 검증 실패 (빈 값)")
    void signup_EmptyEmail_ValidationFailed() throws Exception {
        // given
        MemberRequestDto invalidRequest = new MemberRequestDto();
        invalidRequest.setEmail("");
        invalidRequest.setPassword("Password123");
        invalidRequest.setNickname("testUser");

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).signup(any(MemberRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/auth/signup - 비밀번호 검증 실패 (빈 값)")
    void signup_EmptyPassword_ValidationFailed() throws Exception {
        // given
        MemberRequestDto invalidRequest = new MemberRequestDto();
        invalidRequest.setEmail("test@example.com");
        invalidRequest.setPassword("");
        invalidRequest.setNickname("testUser");

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).signup(any(MemberRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/auth/signup - 중복 이메일로 회원가입 실패")
    void signup_DuplicateEmail_ThrowsException() throws Exception {
        // given
        given(authService.signup(any(MemberRequestDto.class)))
                .willThrow(new yeonjae.snapguide.exception.CustomException(
                        yeonjae.snapguide.exception.ErrorCode.DUPLICATE_USER
                ));

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 가입된 이메일입니다."));

        verify(authService, times(1)).signup(any(MemberRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/auth/login - 존재하지 않는 사용자 로그인 실패")
    void login_UserNotFound_ThrowsException() throws Exception {
        // given
        MemberRequestDto loginRequest = new MemberRequestDto();
        loginRequest.setEmail("nonexistent@example.com");
        loginRequest.setPassword("Password123");

        given(authService.login(any(MemberRequestDto.class)))
                .willThrow(new yeonjae.snapguide.exception.CustomException(
                        yeonjae.snapguide.exception.ErrorCode.USER_NOT_FOUND
                ));

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));

        verify(authService, times(1)).login(any(MemberRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/auth/login - 비밀번호 불일치로 로그인 실패")
    void login_InvalidPassword_ThrowsException() throws Exception {
        // given
        MemberRequestDto loginRequest = new MemberRequestDto();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("WrongPassword1");

        given(authService.login(any(MemberRequestDto.class)))
                .willThrow(new yeonjae.snapguide.exception.CustomException(
                        yeonjae.snapguide.exception.ErrorCode.INVALID_PASSWORD
                ));

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."));

        verify(authService, times(1)).login(any(MemberRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/auth/reissue - 유효하지 않은 토큰으로 재발급 실패")
    void reissue_InvalidToken_ThrowsException() throws Exception {
        // given
        given(authService.reissue(any(TokenRequestDto.class)))
                .willThrow(new yeonjae.snapguide.exception.CustomException(
                        yeonjae.snapguide.exception.ErrorCode.INVALID_TOKEN
                ));

        // when & then
        mockMvc.perform(post("/api/auth/reissue")
                        .with(csrf())
                        .cookie(new Cookie("refresh_token", "invalid-refresh-token")))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("유효하지 않은 토큰입니다."));

        verify(authService, times(1)).reissue(any(TokenRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/auth/reissue - 만료된 리프레시 토큰으로 재발급 실패")
    void reissue_ExpiredRefreshToken_ThrowsException() throws Exception {
        // given
        given(authService.reissue(any(TokenRequestDto.class)))
                .willThrow(new yeonjae.snapguide.exception.CustomException(
                        yeonjae.snapguide.exception.ErrorCode.REFRESH_TOKEN_EXPIRED
                ));

        // when & then
        mockMvc.perform(post("/api/auth/reissue")
                        .with(csrf())
                        .cookie(new Cookie("refresh_token", "expired-refresh-token")))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh Token이 만료되었습니다."));

        verify(authService, times(1)).reissue(any(TokenRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/auth/google/token - OAuth 로그인 실패")
    void exchangeGoogleAuthCode_OAuthFailed_ThrowsException() throws Exception {
        // given
        Map<String, String> request = new HashMap<>();
        request.put("code", "invalid-google-auth-code");

        given(googleOAuthService.exchangeCodeForToken("invalid-google-auth-code"))
                .willThrow(new yeonjae.snapguide.exception.CustomException(
                        yeonjae.snapguide.exception.ErrorCode.OAUTH_LOGIN_FAILED
                ));

        // when & then
        mockMvc.perform(post("/api/auth/google/token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("OAuth 로그인에 실패했습니다."));

        verify(googleOAuthService, times(1)).exchangeCodeForToken("invalid-google-auth-code");
    }

    @Test
    @DisplayName("POST /api/auth/signup - 잘못된 JSON 형식으로 요청")
    void signup_InvalidJsonFormat_BadRequest() throws Exception {
        // given
        String invalidJson = "{email: test@example.com}"; // 따옴표 누락

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).signup(any(MemberRequestDto.class));
    }
}
