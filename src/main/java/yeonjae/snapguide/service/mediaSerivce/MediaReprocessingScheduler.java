package yeonjae.snapguide.service.mediaSerivce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import yeonjae.snapguide.domain.media.Media;
import yeonjae.snapguide.repository.mediaRepository.MediaRepository;
import yeonjae.snapguide.service.fileStorageService.AsyncFileProcessingService;
import yeonjae.snapguide.service.fileStorageService.FileStorageService;

import java.io.InputStream;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * 파생 파일(썸네일/웹용) 생성에 실패했거나(FAILED), 앱 크래시 등으로 처리가
 * 유실된 채 멈춘 건(PENDING + 오래된 lastAttemptAt)을 주기적으로 재시도한다.
 *
 * 재시도 전 markRetryAttempt()로 lastAttemptAt을 먼저 갱신·커밋해두므로,
 * 처리 중(비동기 진행 중)인 건이 다음 스케줄 실행에서 쿨다운 내라면 중복 dispatch 되지 않는다.
 * 원본 재다운로드는 @Scheduled 스레드(기본 단일 스레드)를 막지 않도록 fileProcessingExecutor로 넘긴다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MediaReprocessingScheduler {

    private static final int MAX_RETRY_COUNT = 3;
    private static final int RETRY_COOLDOWN_MINUTES = 5;
    private static final long SCHEDULE_INTERVAL_MS = 5 * 60 * 1000L;
    private static final int MAX_BATCH_SIZE = 50;

    private final MediaRepository mediaRepository;
    private final FileStorageService fileStorageService;
    private final AsyncFileProcessingService asyncFileProcessingService;

    @Qualifier("fileProcessingExecutor")
    private final Executor fileProcessingExecutor;

    @Scheduled(fixedDelay = SCHEDULE_INTERVAL_MS)
    public void retryFailedDerivatives() {
        LocalDateTime cooldownBefore = LocalDateTime.now().minusMinutes(RETRY_COOLDOWN_MINUTES);
        List<Media> candidates = mediaRepository.findRetryCandidates(
                MAX_RETRY_COUNT, cooldownBefore, PageRequest.of(0, MAX_BATCH_SIZE));

        if (candidates.isEmpty()) {
            return;
        }
        log.info("[Reprocess] Found {} media eligible for retry", candidates.size());

        for (Media media : candidates) {
            Long mediaId = media.getId();
            fileProcessingExecutor.execute(() -> retryOne(mediaId));
        }
    }

    private void retryOne(Long mediaId) {
        try {
            String originalKey = markAttemptAndGetOriginalKey(mediaId);
            if (originalKey == null) {
                return; // 그 사이 삭제되었거나 조회 실패 — 다음 스케줄에서 재확인
            }

            byte[] originalBytes;
            try (InputStream in = fileStorageService.downloadFile(originalKey).getInputStream()) {
                originalBytes = in.readAllBytes();
            }

            String baseFileName = extractBaseFileName(originalKey);
            log.info("[Reprocess] Retrying mediaId={}", mediaId);
            asyncFileProcessingService.generateDerivativesAsync(mediaId, baseFileName, originalBytes);
        } catch (Exception e) {
            log.error("[Reprocess] Failed to dispatch retry for mediaId={}: {}", mediaId, e.getMessage());
        }
    }

    /**
     * 재시도 횟수·시각을 먼저 커밋해 중복 dispatch를 막은 뒤, 원본 재조회에 필요한 키를 반환한다.
     */
    private String markAttemptAndGetOriginalKey(Long mediaId) {
        return mediaRepository.findById(mediaId)
                .map(media -> {
                    media.markRetryAttempt();
                    mediaRepository.save(media);
                    return media.getOriginalKey();
                })
                .orElse(null);
    }

    private String extractBaseFileName(String originalKey) {
        String fileName = Paths.get(originalKey).getFileName().toString();
        int dotIdx = fileName.lastIndexOf('.');
        return dotIdx > 0 ? fileName.substring(0, dotIdx) : fileName;
    }
}
