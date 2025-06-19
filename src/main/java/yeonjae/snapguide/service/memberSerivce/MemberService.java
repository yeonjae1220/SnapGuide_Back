package yeonjae.snapguide.service.memberSerivce;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.domain.member.LoginType;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.domain.member.dto.MemberDto;
import yeonjae.snapguide.domain.member.dto.MemberRequestDto;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * TODO : 	•	JWT 또는 세션 로그인 기능 추가
 * 	•	Spring Security + OAuth2 연동
 * 	•	이메일 인증, 비밀번호 변경, 닉네임 중복 체크 등 부가 기능 추가
 */
@Service
@Transactional
//@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public List<MemberDto> getAllMembers() {
        return memberRepository.findAll().stream()
                .map(member -> new MemberDto(member.getId(), member.getEmail()))
                .collect(Collectors.toList());
    }
}
