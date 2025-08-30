package yeonjae.snapguide.repository.guideLikeRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import yeonjae.snapguide.domain.guide.Guide;
import yeonjae.snapguide.domain.like.GuideLike;
import yeonjae.snapguide.domain.member.Member;

import java.util.List;
import java.util.Optional;

public interface GuideLikeRepository extends JpaRepository<GuideLike, Long> {
    // 사용자와 게시물로 좋아요 기록 찾기
    Optional<GuideLike> findByMemberAndGuide(Member member, Guide guide);

    List<GuideLike> findAllByMember(Member member);

    List<GuideLike> findAllByGuide(Guide guide);
}
