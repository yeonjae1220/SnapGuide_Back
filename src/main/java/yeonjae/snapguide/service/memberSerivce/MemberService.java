package yeonjae.snapguide.service.memberSerivce;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.domain.member.LoginType;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.domain.member.dto.LocalSignInRequestDto;
import yeonjae.snapguide.domain.member.dto.LocalSignUpRequestDto;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;

/**
 * TODO : 	•	JWT 또는 세션 로그인 기능 추가
 * 	•	Spring Security + OAuth2 연동
 * 	•	이메일 인증, 비밀번호 변경, 닉네임 중복 체크 등 부가 기능 추가
 */
@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public Long signUp(LocalSignUpRequestDto request) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        Member member = Member.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .loginType(LoginType.LOCAL)
                .build();

        return memberRepository.save(member).getId();
    }

    public Member signIn(LocalSignInRequestDto request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return member;
    }


}
