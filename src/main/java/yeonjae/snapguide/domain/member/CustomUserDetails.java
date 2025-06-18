package yeonjae.snapguide.domain.member;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;
/*
다른곳에서 사용하는 예제를 보게되면, UserDetails를 실제 사용하는 Member 엔티티나, User 엔티티에 상속해서 사용하는 경우도 있다.

하지만, 위의 방법으로 사용하게 된다면 실제 유저를 담는 엔티티가 오염되어, 구분하기 힘들 수 있고, 사용이 어려워질 수 있기에
따로 CustomUserDetails란 클래스를 생성해서 해당 클래스에 UserDetails를 상속받아 사용했다.
 */
@Getter
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final Member member;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return member.getAuthority().stream()
                .map(a -> new SimpleGrantedAuthority(a.name())) // 또는 a.getAuthority()
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return member.getPassword(); // 암호화된 비밀번호
    }

    @Override
    public String getUsername() {
        return member.getEmail(); // 또는 member.getUsername(), 로그인 식별자로 사용되는 값
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
