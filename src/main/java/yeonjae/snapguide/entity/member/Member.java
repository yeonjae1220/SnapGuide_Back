package yeonjae.snapguide.entity.member;

import jakarta.persistence.*;
import lombok.*;
import yeonjae.snapguide.entity.guide.Comment;
import yeonjae.snapguide.entity.guide.Guide;
import yeonjae.snapguide.infrastructure.persistence.jpa.entity.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "username"})
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false, insertable = false, updatable = false)
    private String hashcode;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private Authority authority;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus accountStatus;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private MembershipType membershipType;

    @Embedded
    private ProfileImage profileImage;


}
