package yeonjae.snapguide.security.authentication;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import yeonjae.snapguide.domain.member.Authority;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;

import java.util.List;

/**
 * Admin SSR form login 전용 UserDetailsService.
 * ADMIN authority + 로컬 비밀번호가 설정된 계정만 허용한다.
 * OAuth 전용 계정(password = null)은 명시적으로 차단한다.
 */
@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return memberRepository.findByEmailWithAuthority(email)
                .filter(m -> m.getAuthority() != null
                        && m.getAuthority().contains(Authority.ADMIN))  // ADMIN authority 필터
                .filter(m -> m.getPassword() != null)                   // OAuth 계정(password=null) 차단
                .map(m -> User.builder()
                        .username(m.getEmail())
                        .password(m.getPassword())
                        .authorities(List.of(new SimpleGrantedAuthority("ADMIN")))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("인증에 실패했습니다"));
    }
}
