package yeonjae.snapguide.service.guideSerivce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import yeonjae.snapguide.domain.guide.event.GuideDeletedEvent;
import yeonjae.snapguide.service.fileStorageService.FileStorageService;

import java.io.IOException;

/**
 * Observer(Event) 패턴 - GuideDeletedEvent 수신 시 스토리지 파일 정리.
 * @TransactionalEventListener(AFTER_COMMIT): DB 트랜잭션이 커밋된 후에만 실행되므로
 * DB 롤백 시 파일이 잘못 삭제되는 문제를 방지.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MediaFileCleanupListener {

    private final FileStorageService fileStorageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGuideDeleted(GuideDeletedEvent event) {
        log.info("[MediaFileCleanup] Guide 삭제 이벤트 수신, 파일 {} 건 정리 시작", event.mediaFiles().size());

        for (GuideDeletedEvent.MediaFileInfo fileInfo : event.mediaFiles()) {
            safeDelete(fileInfo.originalKey());
            safeDelete(fileInfo.webKey());
            safeDelete(fileInfo.thumbnailKey());
        }

        log.info("[MediaFileCleanup] 파일 정리 완료");
    }

    private void safeDelete(String key) {
        if (key == null || key.isBlank()) return;
        try {
            fileStorageService.deleteFile(key);
            log.debug("[MediaFileCleanup] 삭제 성공: {}", key);
        } catch (IOException e) {
            log.error("[MediaFileCleanup] 파일 삭제 실패 (무시하고 계속): {}", key, e);
        }
    }
}
