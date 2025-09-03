package yeonjae.snapguide.repository.guideRepository;

import com.querydsl.core.Tuple;
//import com.querydsl.core.group.GroupBy;
import com.querydsl.core.group.GroupBy;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import yeonjae.snapguide.controller.guideController.guideDto.GuideResponseDto;
import yeonjae.snapguide.domain.guide.Guide;
import yeonjae.snapguide.domain.guide.QGuide;
import yeonjae.snapguide.domain.like.QGuideLike;
import yeonjae.snapguide.domain.location.QLocation;
import yeonjae.snapguide.domain.media.MediaDto;
import yeonjae.snapguide.domain.media.QMedia;
import yeonjae.snapguide.domain.member.dto.MemberDto;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Repository
public class GuideRepositoryCustomImpl implements GuideRepositoryCustom{

    private final JPAQueryFactory queryFactory;

//    @Override
//    public List<GuideResponseDto> findAllByMemberId(Long memberId) {
//        QGuide g = QGuide.guide;
//        QMedia m = QMedia.media;
//        QLocation l = QLocation.location;
//        QGuideLike gl = QGuideLike.guideLike; // GuideLike Q-Type 추가
//
//        // ① 가이드 기본 정보 + likeCount + userHasLiked를 한 번에 fetch
//        List<Tuple> guidesWithDetails = queryFactory
//                .select(
//                        g.id,
//                        g.tip,
//                        g.author, // NOTE: DTO 생성에 필요하므로 SELECT 절에 author를 명시적으로 포함
//                        l.formattedAddress,
//                        g.likeCount, // likeCount 추가
//                        // userHasLiked는 서브쿼리로 계산
//                        JPAExpressions
//                                .select(gl.count())
//                                .from(gl)
//                                .where(
//                                        gl.guide.id.eq(g.id),
//                                        gl.member.id.eq(memberId) // 현재 사용자가 좋아요 눌렀는지 확인
//                                )
//                                .gt(0L) // 0보다 크면 true
//                )
//                .from(g)
//                .leftJoin(g.location, l)
//                .where(g.author.id.eq(memberId)) // 특정 작성자의 가이드만 필터링
//                .fetch();
//
//        // 조회된 가이드가 없으면 빈 리스트를 즉시 반환
//        if (guidesWithDetails.isEmpty()) {
//            log.info("[GuideRepositoryCustomImpl] No guides found for authorId: " + memberId);
//            return List.of();
//        }
//
//        // ② 가이드 id 모아 1쿼리로 미디어 조회 (N + 1 방지)
//        List<Long> guideIds = guidesWithDetails.stream()
//                .map(t -> t.get(g.id))
//                .toList();
//
//        Map<Long, List<MediaDto>> mediaMap = queryFactory
//                .select(m.guide.id, m.mediaUrl)
//                .from(m)
//                .where(m.guide.id.in(guideIds))
//                .orderBy(m.id.asc())
//                .transform(GroupBy.groupBy(m.guide.id).as(
//                        GroupBy.list(Projections.constructor(MediaDto.class, m.mediaName, m.mediaUrl))
//                ));
//
//        // ③ DTO 매핑 (모든 필드를 생성자에 전달)
//        return guidesWithDetails.stream()
//                .map(t -> new GuideResponseDto(
//                        t.get(g.id),
//                        t.get(g.tip),
//                        t.get(g.author), // SELECT 절에 추가했으므로 이제 안전하게 사용 가능
//                        t.get(l.formattedAddress),
//                        mediaMap.getOrDefault(t.get(g.id), List.of()),
//                        t.get(g.likeCount), // 조회한 likeCount 전달
//                        Boolean.TRUE.equals(t.get(5, Boolean.class)) // 6번째 요소(인덱스 5)인 userHasLiked 전달
//                )).toList();
//
//    }

    @Override
    public List<GuideResponseDto> findAllByMemberId(Long memberId) {
        QGuide g = QGuide.guide;
        QMedia m = QMedia.media;
        QLocation l = QLocation.location;
        QGuideLike gl = QGuideLike.guideLike;

        // [변경 1] Fetch Join을 사용하여 Guide와 연관된 author, location을 즉시 로딩합니다.
        // Fetch Join을 사용하면 select 절에 엔티티 자체(g)를 명시하고, 반환 타입은 List<Guide>가 됩니다.
        List<Guide> guides = queryFactory
                .select(g)
                .from(g)
                .leftJoin(g.author).fetchJoin() // author를 함께 fetch
                .leftJoin(g.location, l).fetchJoin() // location을 함께 fetch
                .where(g.author.id.eq(memberId))
                .fetch();

        // 조회된 가이드가 없으면 빈 리스트를 즉시 반환
        if (guides.isEmpty()) {
            log.info("[GuideRepositoryCustomImpl] No guides found for authorId: " + memberId);
            return List.of();
        }

        // [변경 2] DTO 매핑을 위해 현재 사용자가 좋아요를 눌렀는지 여부를 별도로 조회합니다.
        // (한 번의 쿼리로 Set에 담아 메모리에서 확인하는 것이 효율적입니다)
        Set<Long> likedGuideIds;
        likedGuideIds = queryFactory
                .select(gl.guide.id)
                .from(gl)
                .where(gl.member.id.eq(memberId)) // 여기서는 '현재 사용자' ID를 써야 하지만, 메서드 시그니처상 '작성자' ID를 사용합니다.
                .fetch()
                .stream().collect(Collectors.toSet());

        // ② 가이드 id 모아 1쿼리로 미디어 조회 (기존과 동일)
        List<Long> guideIds = guides.stream()
                .map(Guide::getId)
                .toList();

        Map<Long, List<MediaDto>> mediaMap = queryFactory
                .select(m.guide.id, m.mediaUrl)
                .from(m)
                .where(m.guide.id.in(guideIds))
                .orderBy(m.id.asc())
                .transform(GroupBy.groupBy(m.guide.id).as(
                        GroupBy.list(Projections.constructor(MediaDto.class, m.mediaName, m.mediaUrl))
                ));

        // =================================================================================
        // 🔹 변경점 3: DTO 매핑 시, 엔티티를 직접 DTO로 변환하여 프록시 문제를 원천 차단합니다.
        //    - Fetch Join 덕분에 guide.getAuthor()는 이제 실제 데이터가 담긴 객체입니다.
        // =================================================================================
        return guides.stream()
                .map(guide -> new GuideResponseDto(
                        guide.getId(),
                        guide.getTip(),
                        // 세션이 살아있을 때 Member 엔티티를 AuthorDto로 즉시 변환
                        MemberDto.fromEntity(guide.getAuthor()),
                        guide.getLocation() != null ? guide.getLocation().getLocationName() : "no name", // NOTE : formattedAddress를 보내는게 낫지않나..?
                        mediaMap.getOrDefault(guide.getId(), List.of()),
                        guide.getLikeCount(),
                        // 메모리에 저장된 Set으로 좋아요 여부를 빠르게 확인
                        likedGuideIds.contains(guide.getId())
                )).toList();
    }



}
