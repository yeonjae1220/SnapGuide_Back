package yeonjae.snapguide.service.guideSerivce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.domain.guide.Guide;
import yeonjae.snapguide.domain.like.GuideLike;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.repository.guideLikeRepository.GuideLikeRepository;
import yeonjae.snapguide.repository.guideRepository.GuideRepository;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;

/**
 * SRP 적용 — 좋아요 토글/조회 책임만 담당.
 * GuideService에서 분리.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class GuideLikeService {

    private final GuideLikeRepository guideLikeRepository;
    private final GuideRepository guideRepository;
    private final MemberRepository memberRepository;

    /**
     * 좋아요 토글 (원자적 업데이트로 동시성 문제 해결)
     * @return true=좋아요 추가, false=좋아요 취소
     */
    public boolean toggleLike(Long guideId, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("로그인이 필요한 서비스입니다.");
        }

        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("사용자 정보를 찾을 수 없습니다."));

        boolean likeExists = guideLikeRepository.existsByMemberIdAndGuideId(member.getId(), guideId);

        if (likeExists) {
            guideLikeRepository.deleteByMemberIdAndGuideId(member.getId(), guideId);
            guideRepository.decrementLikeCount(guideId);
            return false;
        } else {
            Guide guideRef = guideRepository.getReferenceById(guideId);
            guideLikeRepository.save(new GuideLike(member, guideRef));
            guideRepository.incrementLikeCount(guideId);
            return true;
        }
    }

    /**
     * 특정 사용자의 좋아요 여부 조회
     */
    @Transactional(readOnly = true)
    public boolean hasUserLiked(Member member, Guide guide) {
        return guideLikeRepository.findByMemberAndGuide(member, guide).isPresent();
    }
}
