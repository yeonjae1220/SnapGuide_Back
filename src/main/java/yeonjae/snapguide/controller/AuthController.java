package yeonjae.snapguide.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import yeonjae.snapguide.domain.member.dto.MemberRequestDto;
import yeonjae.snapguide.security.authentication.jwt.JwtToken;
import yeonjae.snapguide.security.authentication.jwt.TokenRequestDto;
import yeonjae.snapguide.service.AuthService;
import yeonjae.snapguide.service.GoogleOAuthService;
import yeonjae.snapguide.service.memberSerivce.MemberService;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final long REFRESH_COOKIE_MAX_AGE = 30L * 24 * 60 * 60;

    private final AuthService authService;
    private final MemberService memberService;
    private final RedisTemplate<String, String> redisTemplate;
    private final GoogleOAuthService googleOAuthService;
    private final Environment environment;

    @PostMapping("/signup")
    public ResponseEntity<?> localSignup(@RequestBody @Valid MemberRequestDto request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid MemberRequestDto request, HttpServletResponse response) {
        JwtToken token = authService.login(request);
        addRefreshCookie(response, token.getRefreshToken());
        return ResponseEntity.ok(Map.of(
                "grantType", token.getGrantType(),
                "accessToken", token.getAccessToken(),
                "accessTokenExpiresIn", token.getAccessTokenExpiresIn() != null ? token.getAccessTokenExpiresIn() : 0
        ));
    }

    @PostMapping("/reissue")
    public ResponseEntity<?> reissue(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String cookieRefreshToken,
            @RequestBody Map<String, String> body,
            HttpServletResponse response) {
        TokenRequestDto dto = new TokenRequestDto();
        dto.setAccessToken(body.get("accessToken"));
        dto.setRefreshToken(cookieRefreshToken);
        JwtToken token = authService.reissue(dto);
        if (token.getRefreshToken() != null) {
            addRefreshCookie(response, token.getRefreshToken());
        }
        return ResponseEntity.ok(Map.of(
                "grantType", token.getGrantType(),
                "accessToken", token.getAccessToken(),
                "accessTokenExpiresIn", token.getAccessTokenExpiresIn() != null ? token.getAccessTokenExpiresIn() : 0
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String cookieRefreshToken,
            @RequestBody Map<String, String> body,
            HttpServletResponse response) {
        TokenRequestDto dto = new TokenRequestDto();
        dto.setAccessToken(body.get("accessToken"));
        dto.setRefreshToken(cookieRefreshToken != null ? cookieRefreshToken : "");
        authService.logout(dto);
        clearRefreshCookie(response);
        return ResponseEntity.ok("로그아웃 되었습니다.");
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteMember(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String cookieRefreshToken,
            @RequestBody Map<String, String> body,
            HttpServletResponse response) {
        TokenRequestDto dto = new TokenRequestDto();
        dto.setAccessToken(body.get("accessToken"));
        dto.setRefreshToken(cookieRefreshToken != null ? cookieRefreshToken : "");
        String email = authService.logout(dto);
        memberService.deleteMember(email);
        clearRefreshCookie(response);
        return ResponseEntity.ok("탈퇴 처리 되었습니다.");
    }

    @Profile("local")
    @GetMapping("/test")
    public ResponseEntity<String> test(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok("인증된 사용자: " + userDetails.getUsername());
    }

    @PostMapping("/google/token")
    public ResponseEntity<?> exchangeGoogleAuthCode(@RequestBody Map<String, String> request,
            HttpServletResponse response) {
        String code = request.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Authorization code is required"));
        }
        JwtToken token = googleOAuthService.exchangeCodeForToken(code);
        addRefreshCookie(response, token.getRefreshToken());
        return ResponseEntity.ok(Map.of(
                "grantType", token.getGrantType(),
                "accessToken", token.getAccessToken(),
                "accessTokenExpiresIn", token.getAccessTokenExpiresIn() != null ? token.getAccessTokenExpiresIn() : 0
        ));
    }

    @PostMapping("/oauth/token")
    public ResponseEntity<?> exchangeOAuth2Code(@RequestBody Map<String, String> request,
            HttpServletResponse response) {
        String code = request.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Authorization code is required"));
        }
        JwtToken token = authService.exchangeOAuth2Code(code);
        addRefreshCookie(response, token.getRefreshToken());
        return ResponseEntity.ok(Map.of(
                "grantType", token.getGrantType(),
                "accessToken", token.getAccessToken(),
                "accessTokenExpiresIn", token.getAccessTokenExpiresIn() != null ? token.getAccessTokenExpiresIn() : 0
        ));
    }

    private void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(isProd())
                .sameSite("Lax")
                .maxAge(REFRESH_COOKIE_MAX_AGE)
                .path("/api/auth")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(isProd())
                .sameSite("Lax")
                .maxAge(0)
                .path("/api/auth")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private boolean isProd() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"));
    }
}
