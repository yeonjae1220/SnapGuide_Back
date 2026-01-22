package yeonjae.snapguide.repository.guideRepository;

import com.querydsl.core.BooleanBuilder;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
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

    @PersistenceContext
    private EntityManager entityManager;

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

    @Override
    public Slice<GuideResponseDto> findNearbyGuidesPaged(
            List<Long> locationIds,
            Long lastGuideId,
            int size,
            Long currentMemberId
    ) {
        QGuide g = QGuide.guide;
        QMedia m = QMedia.media;
        QLocation l = QLocation.location;
        QGuideLike gl = QGuideLike.guideLike;

        log.info("🔍 [findNearbyGuidesPaged] locationIds: {}, lastGuideId: {}, size: {}",
                locationIds != null ? locationIds.size() : 0, lastGuideId, size);

        // 1. WHERE 조건 빌더 (locationIds + 커서)
        BooleanBuilder whereClause = new BooleanBuilder();

        if (locationIds != null && !locationIds.isEmpty()) {
            whereClause.and(g.location.id.in(locationIds));
        }

        // 커서 조건: lastGuideId보다 큰 ID만 조회
        if (lastGuideId != null) {
            whereClause.and(g.id.gt(lastGuideId));
        }

        // 2. Guide 조회 (size + 1개 조회하여 hasNext 판단)
        List<Guide> guides = queryFactory
                .select(g)
                .from(g)
                .leftJoin(g.author).fetchJoin()  // N+1 방지
                .leftJoin(g.location, l).fetchJoin()  // N+1 방지
                .where(whereClause)
                .orderBy(g.id.asc())  // ✅ 커서 페이징은 ID 정렬 필수
                .limit(size + 1)  // hasNext 확인용 +1
                .fetch();

        log.info("📘 조회된 Guide 수: {} (limit: {})", guides.size(), size + 1);

        // 3. hasNext 판단 및 실제 데이터 자르기
        boolean hasNext = guides.size() > size;
        if (hasNext) {
            guides = guides.subList(0, size);
        }

        if (guides.isEmpty()) {
            log.info("✅ 조회된 가이드 없음 - 빈 Slice 반환");
            return new SliceImpl<>(List.of(), PageRequest.of(0, size), false);
        }

        // 4. 좋아요 정보 조회 (현재 사용자가 좋아요한 가이드 ID 목록)
        Set<Long> likedGuideIds = Set.of();
        if (currentMemberId != null) {
            List<Long> guideIds = guides.stream()
                    .map(Guide::getId)
                    .toList();

            likedGuideIds = queryFactory
                    .select(gl.guide.id)
                    .from(gl)
                    .where(
                            gl.member.id.eq(currentMemberId),
                            gl.guide.id.in(guideIds)
                    )
                    .fetch()
                    .stream()
                    .collect(Collectors.toSet());

            log.info("❤️ 사용자 {}가 좋아요한 가이드 수: {}", currentMemberId, likedGuideIds.size());
        }

        // 5. Media 조회 (N+1 방지)
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

        log.info("📷 Media 조회 완료 - {} 개 가이드에 미디어 매핑", mediaMap.size());

        // 6. DTO 변환
        Set<Long> finalLikedGuideIds = likedGuideIds;
        List<GuideResponseDto> content = guides.stream()
                .map(guide -> new GuideResponseDto(
                        guide.getId(),
                        guide.getTip(),
                        MemberDto.fromEntity(guide.getAuthor()),
                        guide.getLocation() != null ? guide.getLocation().getLocationName() : "no name",
                        mediaMap.getOrDefault(guide.getId(), List.of()),
                        guide.getLikeCount(),
                        finalLikedGuideIds.contains(guide.getId())
                ))
                .toList();

        log.info("✅ [findNearbyGuidesPaged] 반환 DTO 수: {}, hasNext: {}", content.size(), hasNext);

        return new SliceImpl<>(content, PageRequest.of(0, size), hasNext);
    }

    @Override
    public Slice<GuideResponseDto> findNearbyGuidesPagedOptimized(
            double lat,
            double lng,
            double radiusInDegrees,
            double minLat,
            double minLng,
            double maxLat,
            double maxLng,
            Long lastGuideId,
            int size,
            Long currentMemberId
    ) {
        log.info("🚀 [findNearbyGuidesPagedOptimized] lat: {}, lng: {}, radius: {}, cursor: {}, size: {}",
                lat, lng, radiusInDegrees, lastGuideId, size);

        // 🔹 통합 쿼리: PostGIS 공간 검색 + Guide JOIN을 단일 쿼리로 처리
        // - 기존: Location 조회(120개) → IN 절로 Guide 조회 (느림)
        // - 개선: 공간 조건으로 Guide를 직접 조회 (빠름)
        String sql = """
            SELECT g.id
            FROM guide g
            INNER JOIN location l ON g.location_id = l.id
            WHERE l.coordinate && ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)
              AND ST_DWithin(l.coordinate, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326), :radiusInDegrees)
              AND (:cursor IS NULL OR g.id > :cursor)
            ORDER BY g.id ASC
            LIMIT :limit
            """;

        // Native Query 실행 (Guide ID만 조회)
        @SuppressWarnings("unchecked")
        List<Long> guideIds = entityManager.createNativeQuery(sql)
                .setParameter("lat", lat)
                .setParameter("lng", lng)
                .setParameter("radiusInDegrees", radiusInDegrees)
                .setParameter("minLat", minLat)
                .setParameter("minLng", minLng)
                .setParameter("maxLat", maxLat)
                .setParameter("maxLng", maxLng)
                .setParameter("cursor", lastGuideId)
                .setParameter("limit", size + 1)  // hasNext 확인용 +1
                .getResultList()
                .stream()
                .map(id -> ((Number) id).longValue())
                .toList();

        log.info("📍 공간 쿼리 결과 - Guide ID 수: {} (limit: {})", guideIds.size(), size + 1);

        // hasNext 판단
        boolean hasNext = guideIds.size() > size;
        List<Long> actualGuideIds = hasNext ? guideIds.subList(0, size) : guideIds;

        if (actualGuideIds.isEmpty()) {
            log.info("✅ 조회된 가이드 없음 - 빈 Slice 반환");
            return new SliceImpl<>(List.of(), PageRequest.of(0, size), false);
        }

        // 🔹 조회된 Guide ID로 엔티티 Fetch (Fetch Join 사용)
        QGuide g = QGuide.guide;
        QLocation l = QLocation.location;

        List<Guide> guides = queryFactory
                .select(g)
                .from(g)
                .leftJoin(g.author).fetchJoin()  // N+1 방지
                .leftJoin(g.location, l).fetchJoin()  // N+1 방지
                .where(g.id.in(actualGuideIds))
                .orderBy(g.id.asc())
                .fetch();

        log.info("📘 Guide 엔티티 조회 완료 - 수: {}", guides.size());

        // 🔹 좋아요 정보 조회
        Set<Long> likedGuideIds = Set.of();
        if (currentMemberId != null) {
            QGuideLike gl = QGuideLike.guideLike;
            likedGuideIds = queryFactory
                    .select(gl.guide.id)
                    .from(gl)
                    .where(
                            gl.member.id.eq(currentMemberId),
                            gl.guide.id.in(actualGuideIds)
                    )
                    .fetch()
                    .stream()
                    .collect(Collectors.toSet());

            log.info("❤️ 사용자 {}가 좋아요한 가이드 수: {}", currentMemberId, likedGuideIds.size());
        }

        // 🔹 Media 조회 (N+1 방지)
        QMedia m = QMedia.media;
        Map<Long, List<MediaDto>> mediaMap = queryFactory
                .select(m.guide.id, m.mediaUrl)
                .from(m)
                .where(m.guide.id.in(actualGuideIds))
                .orderBy(m.id.asc())
                .transform(GroupBy.groupBy(m.guide.id).as(
                        GroupBy.list(Projections.constructor(MediaDto.class, m.mediaName, m.mediaUrl))
                ));

        log.info("📷 Media 조회 완료 - {} 개 가이드에 미디어 매핑", mediaMap.size());

        // 🔹 DTO 변환
        Set<Long> finalLikedGuideIds = likedGuideIds;
        List<GuideResponseDto> content = guides.stream()
                .map(guide -> new GuideResponseDto(
                        guide.getId(),
                        guide.getTip(),
                        MemberDto.fromEntity(guide.getAuthor()),
                        guide.getLocation() != null ? guide.getLocation().getLocationName() : "no name",
                        mediaMap.getOrDefault(guide.getId(), List.of()),
                        guide.getLikeCount(),
                        finalLikedGuideIds.contains(guide.getId())
                ))
                .toList();

        log.info("✅ [findNearbyGuidesPagedOptimized] 반환 DTO 수: {}, hasNext: {}", content.size(), hasNext);

        return new SliceImpl<>(content, PageRequest.of(0, size), hasNext);
    }

}
