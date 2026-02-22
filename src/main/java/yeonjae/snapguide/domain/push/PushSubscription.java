package yeonjae.snapguide.domain.push;

import jakarta.persistence.*;
import lombok.*;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.infrastructure.persistence.jpa.entity.BaseEntity;

@Entity
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushSubscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, unique = true)
    private String endpoint;

    @Column(nullable = false)
    private String auth;

    @Column(nullable = false)
    private String p256dh;

    @Builder.Default
    private boolean active = true;

    public void deactivate() {
        this.active = false;
    }
}
