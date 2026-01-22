package yeonjae.snapguide.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import yeonjae.snapguide.domain.member.CustomUserDetails;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.exception.CustomException;
import yeonjae.snapguide.exception.ErrorCode;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // ✅ authority를 함께 조회하여 N+1 방지
        return memberRepository.findByEmailWithAuthority(email)
                .map(this::createUserDetails)
                // DB에 유효하지 않은 유저로 로그인 시도 했을 경우 Exception
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    }

    // DB 에 User 값이 존재한다면 UserDetails 객체로 만들어서 리턴
    private UserDetails createUserDetails(Member member) {
        return new CustomUserDetails(member);
    }
}
