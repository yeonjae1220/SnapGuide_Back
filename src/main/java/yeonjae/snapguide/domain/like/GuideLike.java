package yeonjae.snapguide.domain.like;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import yeonjae.snapguide.domain.guide.Guide;
import yeonjae.snapguide.domain.member.Member;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GuideLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", nullable = false)
    private Guide guide;

    public GuideLike(Member member, Guide guide) {
        this.member = member;
        this.guide = guide;
    }
}
