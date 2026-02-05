package yeonjae.snapguide.repository.guideLikeRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeonjae.snapguide.domain.guide.Guide;
import yeonjae.snapguide.domain.like.GuideLike;
import yeonjae.snapguide.domain.member.Member;

import java.util.List;
import java.util.Optional;

public interface GuideLikeRepository extends JpaRepository<GuideLike, Long> {
    // 사용자와 게시물로 좋아요 기록 찾기
    Optional<GuideLike> findByMemberAndGuide(Member member, Guide guide);

    /**
     * ID 기반 좋아요 조회 (프록시 초기화 없이 직접 쿼리)
     * 엔티티 대신 ID만 사용하여 불필요한 SELECT 쿼리 방지
     */
    Optional<GuideLike> findByMemberIdAndGuideId(Long memberId, Long guideId);

    /**
     * ID 기반 좋아요 삭제 (SELECT 없이 바로 DELETE)
     * @Query + @Modifying으로 SELECT 쿼리 없이 직접 DELETE 실행
     */
    @Modifying
    @Query("DELETE FROM GuideLike gl WHERE gl.member.id = :memberId AND gl.guide.id = :guideId")
    void deleteByMemberIdAndGuideId(@Param("memberId") Long memberId, @Param("guideId") Long guideId);

    /**
     * 좋아요 존재 여부 확인 (COUNT 쿼리로 최적화)
     */
    boolean existsByMemberIdAndGuideId(Long memberId, Long guideId);

    List<GuideLike> findAllByMember(Member member);

    List<GuideLike> findAllByGuide(Guide guide);
}
