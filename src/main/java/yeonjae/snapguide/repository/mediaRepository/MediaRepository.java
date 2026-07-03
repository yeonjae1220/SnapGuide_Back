package yeonjae.snapguide.repository.mediaRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeonjae.snapguide.domain.media.Media;

import java.time.LocalDateTime;
import java.util.List;

public interface MediaRepository extends JpaRepository<Media, Long>, MediaRepositoryCustom {
    @Query("SELECT l.id FROM Media m JOIN m.location l WHERE m.id IN :mediaIds AND l IS NOT NULL")
    List<Long> findFirstLocationIdByMediaIds(@Param("mediaIds") List<Long> mediaIds, Pageable pageable);

    /**
     * 파생 파일 생성 재시도 대상 조회.
     * - FAILED: 실패한 건
     * - PENDING + lastAttemptAt 오래됨: 앱 크래시 등으로 비동기 처리가 유실된 건
     * cooldownBefore 이후에 시도된 건은 아직 처리 중이거나 최근 재시도된 것으로 보고 제외한다.
     * pageable로 한 번의 스윕에서 처리할 건수를 제한한다(장애 후 대량 폭주 방지 — 못 담은 건은 다음 스케줄에서 재조회).
     */
    @Query("SELECT m FROM Media m WHERE m.retryCount < :maxRetryCount " +
           "AND m.processingStatus IN (yeonjae.snapguide.domain.media.ProcessingStatus.FAILED, " +
           "yeonjae.snapguide.domain.media.ProcessingStatus.PENDING) " +
           "AND (m.lastAttemptAt IS NULL OR m.lastAttemptAt < :cooldownBefore)")
    List<Media> findRetryCandidates(@Param("maxRetryCount") int maxRetryCount,
                                     @Param("cooldownBefore") LocalDateTime cooldownBefore,
                                     Pageable pageable);
}
