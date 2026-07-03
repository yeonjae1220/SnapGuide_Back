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
