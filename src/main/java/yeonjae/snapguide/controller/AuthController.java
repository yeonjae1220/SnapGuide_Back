package yeonjae.snapguide.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yeonjae.snapguide.domain.member.dto.LocalSignUpRequestDto;
import yeonjae.snapguide.domain.member.dto.MemberRequestDto;
import yeonjae.snapguide.security.authentication.jwt.RefreshTokenDto;
import yeonjae.snapguide.security.authentication.jwt.TokenRequestDto;
import yeonjae.snapguide.service.AuthService;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
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

    @PostMapping("/test")
    public ResponseEntity<?> test(@RequestBody String test) {
        return ResponseEntity.ok("test Controller");
    }
}
/*
{
    "email" : "test1",
    "password" : "test1"
}
 */