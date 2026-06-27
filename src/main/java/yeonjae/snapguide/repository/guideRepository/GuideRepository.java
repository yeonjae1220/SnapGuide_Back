package yeonjae.snapguide.repository.guideRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import yeonjae.snapguide.domain.guide.Guide;


import java.util.List;
import java.util.Optional;

public interface GuideRepository extends JpaRepository<Guide, Long>, GuideRepositoryCustom {

    /**
     * 원자적 좋아요 증가 (동시성 안전)
     * - flushAutomatically: UPDATE 실행 전 영속성 컨텍스트 flush (INSERT 먼저 실행)
     * - clearAutomatically: UPDATE 후 영속성 컨텍스트 clear (캐시 불일치 방지)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Guide g SET g.likeCount = g.likeCount + 1 WHERE g.id = :guideId")
    void incrementLikeCount(@Param("guideId") Long guideId);

    /**
     * 원자적 좋아요 감소 (동시성 안전)
     * - flushAutomatically: UPDATE 실행 전 영속성 컨텍스트 flush (DELETE 먼저 실행)
     * - clearAutomatically: UPDATE 후 영속성 컨텍스트 clear (캐시 불일치 방지)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Guide g SET g.likeCount = g.likeCount - 1 WHERE g.id = :guideId AND g.likeCount > 0")
    void decrementLikeCount(@Param("guideId") Long guideId);
    List<Guide> findByLocationIdIn(List<Long> locationIds);

    /**
     * Fetch Join을 사용하여 Guide와 연관된 Media, Author, Location을 한 번에 조회
     * N+1 문제 해결
     */
    @Query("""
        SELECT DISTINCT g FROM Guide g
        LEFT JOIN FETCH g.mediaList m
        LEFT JOIN FETCH m.mediaMetaData md
        LEFT JOIN FETCH md.cameraModel
        JOIN FETCH g.author
        JOIN FETCH g.location
        WHERE g.location.id IN :locationIds
        """)
    List<Guide> findByLocationIdInWithFetch(@Param("locationIds") List<Long> locationIds);

    /**
     * 지도 축소 시 국가/대륙 단위 집계용 경량 프로젝션.
     * 공개 가이드 + 좌표가 있는 Location만 반환하며 미디어를 제외해 쿼리를 최소화한다.
     */
    @Query(value = """
        SELECT
            g.id           AS guideId,
            l.country_code AS countryCode,
            l.country      AS country,
            ST_Y(l.coordinate) AS lat,
            ST_X(l.coordinate) AS lng,
            g.like_count   AS likeCount
        FROM guide g
        JOIN location l ON g.location_id = l.id
        WHERE g.location_public = true
          AND l.country_code IS NOT NULL
          AND l.coordinate IS NOT NULL
        """, nativeQuery = true)
    List<GuideAggregateRow> findAllForAggregation();

    /**
     * 특정 가이드 ID 목록에서 첫 번째 미디어 URL 조회 (썸네일 전용).
     * guide_id IN (:ids) 로 한 번에 조회 후 서비스에서 groupBy.
     */
    @Query(value = """
        SELECT m.guide_id AS guideId, m.media_url AS mediaUrl
        FROM media m
        JOIN (
            SELECT guide_id, MIN(id) AS media_id
            FROM media
            WHERE guide_id IN :guideIds
            GROUP BY guide_id
        ) first_media
          ON first_media.guide_id = m.guide_id
         AND first_media.media_id = m.id
        """, nativeQuery = true)
    List<MediaThumbnailRow> findFirstMediaUrlsByGuideIds(@Param("guideIds") List<Long> guideIds);

    /**
     * Guide 단건 조회 시 연관 엔티티 함께 조회
     */
    @Query("""
        SELECT DISTINCT g FROM Guide g
        LEFT JOIN FETCH g.mediaList m
        LEFT JOIN FETCH m.mediaMetaData md
        LEFT JOIN FETCH md.cameraModel
        LEFT JOIN FETCH g.author
        LEFT JOIN FETCH g.location
        WHERE g.id = :id
        """)
    Optional<Guide> findByIdWithFetch(@Param("id") Long id);

    /**
     * 공개 영감 피드용 커서 조회. 먼저 ID만 제한 조회한 뒤 fetch join으로 상세를 가져와
     * 컬렉션 fetch join + pagination 경고와 중복 row 제한 문제를 피한다.
     */
    @Query("""
        SELECT g.id FROM Guide g
        WHERE (:cursor IS NULL OR g.id < :cursor)
        ORDER BY g.id DESC
        """)
    List<Long> findFeedIdsBefore(@Param("cursor") Long cursor, Pageable pageable);

    @Query("""
        SELECT DISTINCT g FROM Guide g
        LEFT JOIN FETCH g.mediaList m
        LEFT JOIN FETCH m.mediaMetaData md
        LEFT JOIN FETCH md.cameraModel
        JOIN FETCH g.author
        LEFT JOIN FETCH g.location
        WHERE g.id IN :guideIds
        """)
    List<Guide> findByIdsWithFeedFetch(@Param("guideIds") List<Long> guideIds);

}
