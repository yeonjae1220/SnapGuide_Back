package yeonjae.snapguide.domain.member;

import jakarta.persistence.*;
import lombok.*;
import yeonjae.snapguide.domain.guide.Guide;
import yeonjae.snapguide.infrastructure.persistence.jpa.entity.BaseEntity;

import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Guide> guides = new ArrayList<>();

    // private String loginId; // form 로그인용

    @Column(nullable = false)
    private String email;

    @Column
    private String password; // local login에만 사용 (소셜은 null)

    // @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    private Provider provider; // LOCAL, GOOGLE, KAKAO, APPLE

    private String providerId; // 소셜 로그인 고유 ID

    @Embedded
    private ProfileImage profileImage;

//    @Column(nullable = false, insertable = false, updatable = false)
//    private String hashcode;
//
    @ElementCollection(fetch = FetchType.LAZY)  // ✅ N+1 문제 해결: LAZY로 변경
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
