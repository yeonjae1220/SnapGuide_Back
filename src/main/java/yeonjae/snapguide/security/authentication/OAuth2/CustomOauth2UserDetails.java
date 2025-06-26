package yeonjae.snapguide.security.authentication.OAuth2;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import yeonjae.snapguide.domain.member.Member;

import java.util.Collection;
import java.util.Map;


@Getter
@AllArgsConstructor
public class CustomOauth2UserDetails implements UserDetails, OAuth2User {
    private final Member member;
    private final Map<String, Object> attributes;

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }

    @Override
    public String getName() {
        // OAuth2User용 principal name
        return member.getEmail();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return member.getAuthority();
    }

    @Override
    public String getPassword() {
        // OAuth2 로그인에서는 보통 사용되지 않음
        return null;
    }

    @Override
    public String getUsername() {
        // UserDetails용 principal name
        return member.getEmail();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
