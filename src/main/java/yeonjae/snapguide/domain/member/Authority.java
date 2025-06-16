package yeonjae.snapguide.domain.member;

import org.springframework.security.core.GrantedAuthority;

public enum Authority implements GrantedAuthority {
    MEMBER, ADMIN;

    @Override
    public String getAuthority() {
        //name()은 Enum 클래스에서 기본으로 제공하는 메서드이며, enum 상수의 이름 문자열을 반환
        return name();
    }
}
