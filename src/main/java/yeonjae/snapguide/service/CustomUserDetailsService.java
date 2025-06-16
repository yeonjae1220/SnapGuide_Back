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
import yeonjae.snapguide.domain.member.Member;
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
        return memberRepository.findByEmail(email)
                .map(this::createUserDetails)
                // TODO : 예외처리 필요 .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
                .orElseThrow(RuntimeException::new);
    }

    // DB 에 User 값이 존재한다면 UserDetails 객체로 만들어서 리턴
//    private UserDetails createUserDetails(Member member) {
//        GrantedAuthority grantedAuthority = new SimpleGrantedAuthority(member.getAuthorities().toString());
//
//        return new User(
//                String.valueOf(member.getId()),
//                member.getPassword(),
//                Collections.singleton(grantedAuthority)
//        );
//    }
    private UserDetails createUserDetails(Member member) {
        List<GrantedAuthority> authorities = member.getAuthorities().stream()
                .map(auth -> new SimpleGrantedAuthority(auth.getAuthority()))  // 예: "MEMBER"
                .collect(Collectors.toList());

        return new User(
                String.valueOf(member.getId()),
                member.getPassword(),
                authorities
        );
    }
}
