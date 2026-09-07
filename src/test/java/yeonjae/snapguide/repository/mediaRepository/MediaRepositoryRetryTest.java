package yeonjae.snapguide.repository.mediaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.domain.cameraModel.CameraModel;
import yeonjae.snapguide.domain.media.Media;
import yeonjae.snapguide.domain.media.ProcessingStatus;
import yeonjae.snapguide.domain.mediaMetaData.MediaMetaData;
import yeonjae.snapguide.repository.cameraModelRepository.CameraModelRepository;
import yeonjae.snapguide.repository.mediaMetaDataRepository.MediaMetaDataRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@DisplayName("MediaRepository.findRetryCandidates")
class MediaRepositoryRetryTest {

    @Autowired
    private MediaRepository mediaRepository;
    @Autowired
    private MediaMetaDataRepository mediaMetaDataRepository;
    @Autowired
    private CameraModelRepository cameraModelRepository;

    private static final int MAX_RETRY = 3;
    private LocalDateTime cooldownBefore;

    @BeforeEach
    void setUp() {
        cooldownBefore = LocalDateTime.now().minusMinutes(5);
    }

    private Media persistMedia(ProcessingStatus status, int retryCount, LocalDateTime lastAttemptAt) {
        CameraModel cameraModel = cameraModelRepository.save(
                CameraModel.builder().manufacturer("Test").model("TestModel").build());
        MediaMetaData metaData = mediaMetaDataRepository.save(
                MediaMetaData.builder().cameraModel(cameraModel).build());

        return mediaRepository.save(Media.builder()
                .mediaName("test.jpg")
                .mediaUrl("/media/files/test.jpg")
                .originalKey("images/originals/test.jpg")
                .fileSize(100L)
                .processingStatus(status)
                .retryCount(retryCount)
                .lastAttemptAt(lastAttemptAt)
                .mediaMetaData(metaData)
                .build());
    }

    private List<Media> findCandidates() {
        return mediaRepository.findRetryCandidates(MAX_RETRY, cooldownBefore, PageRequest.of(0, 50));
    }

    @Nested
    @DisplayName("상태 기반 대상 선정")
    class StatusFiltering {

        @Test
        @DisplayName("FAILED 상태이고 쿨다운을 지났으면 재시도 대상에 포함된다")
        void includesFailedPastCooldown() {
            Media media = persistMedia(ProcessingStatus.FAILED, 0, LocalDateTime.now().minusMinutes(10));

            List<Media> result = findCandidates();

            assertThat(result).extracting(Media::getId).contains(media.getId());
        }

        @Test
        @DisplayName("PENDING이지만 lastAttemptAt이 오래되면(크래시로 유실된 건) 재시도 대상에 포함된다")
        void includesStalePending() {
            Media media = persistMedia(ProcessingStatus.PENDING, 0, LocalDateTime.now().minusMinutes(10));

            List<Media> result = findCandidates();

            assertThat(result).extracting(Media::getId).contains(media.getId());
        }

        @Test
        @DisplayName("COMPLETED 상태는 재시도 대상에서 제외된다")
        void excludesCompleted() {
            Media media = persistMedia(ProcessingStatus.COMPLETED, 0, LocalDateTime.now().minusMinutes(10));

            List<Media> result = findCandidates();

            assertThat(result).extracting(Media::getId).doesNotContain(media.getId());
        }

        @Test
        @DisplayName("lastAttemptAt이 null이면(레거시 데이터) 재시도 대상에 포함된다")
        void includesNullLastAttemptAt() {
            Media media = persistMedia(ProcessingStatus.FAILED, 0, null);

            List<Media> result = findCandidates();

            assertThat(result).extracting(Media::getId).contains(media.getId());
        }
    }

    @Nested
    @DisplayName("쿨다운/재시도 횟수 제한")
    class CooldownAndRetryLimit {

        @Test
        @DisplayName("쿨다운 이내에 재시도된 건은 제외된다(처리 중일 수 있으므로)")
        void excludesWithinCooldown() {
            Media media = persistMedia(ProcessingStatus.FAILED, 0, LocalDateTime.now().minusMinutes(1));

            List<Media> result = findCandidates();

            assertThat(result).extracting(Media::getId).doesNotContain(media.getId());
        }

        @Test
        @DisplayName("최대 재시도 횟수에 도달한 건은 제외된다")
        void excludesWhenMaxRetryReached() {
            Media media = persistMedia(ProcessingStatus.FAILED, MAX_RETRY, LocalDateTime.now().minusMinutes(10));

            List<Media> result = findCandidates();

            assertThat(result).extracting(Media::getId).doesNotContain(media.getId());
        }
    }

    @Nested
    @DisplayName("재시도 예산 소진 건의 종료 정산 (GLOBAL-PIT-067 / PIT-143)")
    class ExhaustedSettlement {

        private List<Media> findExhausted() {
            return mediaRepository.findExhaustedPending(MAX_RETRY, cooldownBefore, PageRequest.of(0, 50));
        }

        @Test
        @DisplayName("재시도 예산을 소진했는데 PENDING 이면 정산 대상이다 — 이 건이 예전엔 영원히 '처리 대기 중'이었다")
        void includesExhaustedPending() {
            Media media = persistMedia(ProcessingStatus.PENDING, MAX_RETRY, LocalDateTime.now().minusMinutes(10));

            assertThat(findExhausted()).extracting(Media::getId).contains(media.getId());
            // 재시도 후보에서는 빠져 있다 = 스스로 회복될 길이 없다
            assertThat(findCandidates()).extracting(Media::getId).doesNotContain(media.getId());
        }

        @Test
        @DisplayName("이미 FAILED 로 정산된 건은 다시 정산하지 않는다")
        void excludesAlreadyFailed() {
            Media media = persistMedia(ProcessingStatus.FAILED, MAX_RETRY, LocalDateTime.now().minusMinutes(10));

            assertThat(findExhausted()).extracting(Media::getId).doesNotContain(media.getId());
        }

        @Test
        @DisplayName("예산이 남아 있으면 정산하지 않는다 — 아직 재시도로 회복될 수 있다")
        void excludesWhenBudgetRemains() {
            Media media = persistMedia(ProcessingStatus.PENDING, MAX_RETRY - 1, LocalDateTime.now().minusMinutes(10));

            assertThat(findExhausted()).extracting(Media::getId).doesNotContain(media.getId());
            assertThat(findCandidates()).extracting(Media::getId).contains(media.getId());
        }

        @Test
        @DisplayName("마지막 시도가 쿨다운 이내면 정산하지 않는다 — 그 시도가 아직 끝나지 않았을 수 있다")
        void excludesWithinCooldown() {
            Media media = persistMedia(ProcessingStatus.PENDING, MAX_RETRY, LocalDateTime.now().minusMinutes(1));

            assertThat(findExhausted()).extracting(Media::getId).doesNotContain(media.getId());
        }

        @Test
        @DisplayName("정산하면 FAILED 가 되고 그 뒤로는 재시도 후보에도 정산 대상에도 들지 않는다(종료 상태)")
        void settledIsTerminal() {
            Media media = persistMedia(ProcessingStatus.PENDING, MAX_RETRY, LocalDateTime.now().minusMinutes(10));

            media.markProcessingFailed();
            mediaRepository.saveAndFlush(media);

            assertThat(media.getProcessingStatus()).isEqualTo(ProcessingStatus.FAILED);
            assertThat(findExhausted()).extracting(Media::getId).doesNotContain(media.getId());
            assertThat(findCandidates()).extracting(Media::getId).doesNotContain(media.getId());
        }
    }

    @Nested
    @DisplayName("배치 크기 제한")
    class BatchLimit {

        @Test
        @DisplayName("pageable로 지정한 건수만큼만 반환한다")
        void limitsResultsByPageable() {
            for (int i = 0; i < 5; i++) {
                persistMedia(ProcessingStatus.FAILED, 0, LocalDateTime.now().minusMinutes(10));
            }

            List<Media> result = mediaRepository.findRetryCandidates(MAX_RETRY, cooldownBefore, PageRequest.of(0, 2));

            assertThat(result).hasSize(2);
        }
    }
}
