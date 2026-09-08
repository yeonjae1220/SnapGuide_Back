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

    /**
     * 재시도 예산을 다 쓰고도 PENDING 으로 남은 건.
     *
     * 🔴 findRetryCandidates 는 retryCount 가 한도에 닿으면 그 건을 후보에서 **빼기만** 한다.
     *    retryOne() 이 원본 다운로드 단계에서 던지면 markProcessingFailed() 가 호출되지 않으므로
     *    상태는 PENDING 그대로 남고, 종료 상태로 가는 경로가 없어 클라이언트는 '처리 대기 중'을
     *    영원히 본다. 결정적 실패는 재시도가 아니라 **종료로 정산**해야 한다 (GLOBAL-PIT-143).
     *
     * cooldownBefore 조건을 함께 두는 이유: 마지막 시도가 아직 진행 중일 수 있어,
     * 그 건을 FAILED 로 먼저 찍으면 곧 COMPLETED 가 될 작업을 실패로 오기록한다.
     */
    @Query("SELECT m FROM Media m WHERE m.retryCount >= :maxRetryCount " +
           "AND m.processingStatus = yeonjae.snapguide.domain.media.ProcessingStatus.PENDING " +
           "AND (m.lastAttemptAt IS NULL OR m.lastAttemptAt < :cooldownBefore)")
    List<Media> findExhaustedPending(@Param("maxRetryCount") int maxRetryCount,
                                     @Param("cooldownBefore") LocalDateTime cooldownBefore,
                                     Pageable pageable);
}
