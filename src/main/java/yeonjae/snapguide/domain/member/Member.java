package yeonjae.snapguide.domain.member;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import yeonjae.snapguide.infrastructure.persistence.jpa.entity.BaseEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "email"})
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column
    private String password; // local login에만 사용 (소셜은 null)

    // @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    private LoginType loginType; // LOCAL, GOOGLE, KAKAO, APPLE

    private String providerId; // 소셜 로그인 고유 ID

    @Embedded
    private ProfileImage profileImage;

//    @Column(nullable = false, insertable = false, updatable = false)
//    private String hashcode;
//
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private List<Authority> authority = new ArrayList<>();


//
//    @Enumerated(value = EnumType.STRING)
//    @Column(nullable = false)
//    private AccountStatus accountStatus;
//
//    @Enumerated(value = EnumType.STRING)
//    @Column(nullable = false)
//    private MembershipType membershipType;
//



}
