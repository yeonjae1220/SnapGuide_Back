package yeonjae.snapguide.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import yeonjae.snapguide.domain.member.dto.MemberRequestDto;
import yeonjae.snapguide.security.authentication.jwt.TokenRequestDto;
import yeonjae.snapguide.service.AuthService;
import yeonjae.snapguide.service.memberSerivce.MemberService;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final MemberService memberService;
    private final RedisTemplate<String, String> redisTemplate;
    private final yeonjae.snapguide.service.GoogleOAuthService googleOAuthService;

    @PostMapping("/signup")
    public ResponseEntity<?> localSignup(@RequestBody @Valid MemberRequestDto request) {
        // 회원가입 처리
//        return ResponseEntity.ok(Map.of("message", "회원가입 성공"));
        return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid MemberRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/reissue")
    public ResponseEntity<?> reissue(@RequestBody @Valid TokenRequestDto token) {
        return ResponseEntity.ok(authService.reissue(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody @Valid TokenRequestDto tokenRequestDto) {
        authService.logout(tokenRequestDto);

        return ResponseEntity.ok("로그아웃 되었습니다.");
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteMember(@RequestBody @Valid TokenRequestDto tokenRequestDto) {
        String email = authService.logout(tokenRequestDto); // 여기서 토큰으로 검사
        memberService.deleteMember(email);
        return ResponseEntity.ok("탈퇴 처리 되었습니다.");
    }


    @GetMapping("/test")
    public ResponseEntity<?> test(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok("인증된 사용자: " + userDetails.getUsername());
    }

    /**
     * 모바일 앱에서 Google Authorization Code를 JWT 토큰으로 교환 (옵션 1)
     * - 앱이 Google에서 직접 받은 authorization code를 JWT로 교환
     * - Google API를 통해 사용자 정보 조회 및 회원가입/로그인 처리
     */
    @PostMapping("/google/token")
    public ResponseEntity<?> exchangeGoogleAuthCode(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Authorization code is required"));
        }

        return ResponseEntity.ok(googleOAuthService.exchangeGoogleAuthorizationCode(code));
    }

    /**
     * 웹에서 OAuth2 authorization code를 JWT 토큰으로 교환 (옵션 2 - 기존 방식)
     * - 백엔드가 생성한 일회용 code를 JWT로 교환
     * - code는 Redis에 저장되며 5분간 유효
     */
    @PostMapping("/oauth2/token")
    public ResponseEntity<?> exchangeOAuth2Code(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Authorization code is required"));
        }

        return ResponseEntity.ok(authService.exchangeOAuth2Code(code));
    }
}