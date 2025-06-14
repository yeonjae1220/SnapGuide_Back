package yeonjae.snapguide.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yeonjae.snapguide.domain.member.dto.LocalSignUpRequestDto;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    @PostMapping("/signup")
    public ResponseEntity<?> localSignup(@RequestBody LocalSignUpRequestDto request) {
        // 회원가입 처리
        return ResponseEntity.ok(Map.of("message", "회원가입 성공"));
    }
}
