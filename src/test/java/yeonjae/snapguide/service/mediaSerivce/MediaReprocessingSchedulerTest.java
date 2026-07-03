package yeonjae.snapguide.service.mediaSerivce;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import yeonjae.snapguide.domain.media.Media;
import yeonjae.snapguide.domain.media.ProcessingStatus;
import yeonjae.snapguide.repository.mediaRepository.MediaRepository;
import yeonjae.snapguide.service.fileStorageService.AsyncFileProcessingService;
import yeonjae.snapguide.service.fileStorageService.FileStorageService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("MediaReprocessingScheduler")
@ExtendWith(MockitoExtension.class)
class MediaReprocessingSchedulerTest {

    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private AsyncFileProcessingService asyncFileProcessingService;

    // 테스트에서는 스레드풀 대신 호출 스레드에서 즉시 실행해 동기적으로 검증한다.
    private final Executor immediateExecutor = Runnable::run;

    private MediaReprocessingScheduler scheduler;

    private Media buildFailedMedia(Long id, String originalKey) {
        return Media.builder()
                .id(id)
                .mediaName("test.jpg")
                .mediaUrl("/media/files/test.jpg")
                .originalKey(originalKey)
                .fileSize(100L)
                .processingStatus(ProcessingStatus.FAILED)
                .build();
    }

    @Nested
    @DisplayName("retryFailedDerivatives 메서드")
    class RetryFailedDerivatives {

        @Test
        @DisplayName("재시도 대상이 없으면 다운로드나 재생성 요청을 하지 않는다")
        void doesNothingWhenNoCandidates() {
            scheduler = new MediaReprocessingScheduler(
                    mediaRepository, fileStorageService, asyncFileProcessingService, immediateExecutor);
            given(mediaRepository.findRetryCandidates(anyInt(), any(LocalDateTime.class), any(Pageable.class)))
                    .willReturn(List.of());

            scheduler.retryFailedDerivatives();

            verifyNoInteractions(fileStorageService, asyncFileProcessingService);
        }

        @Test
        @DisplayName("재시도 대상 건마다 재시도 횟수를 올리고 원본을 재조회해 파생 파일 생성을 재요청한다")
        void redispatchesDerivativeGenerationForEachCandidate() throws IOException {
            scheduler = new MediaReprocessingScheduler(
                    mediaRepository, fileStorageService, asyncFileProcessingService, immediateExecutor);
            Media media = buildFailedMedia(1L, "images/originals/abc-123.jpg");

            given(mediaRepository.findRetryCandidates(anyInt(), any(LocalDateTime.class), any(Pageable.class)))
                    .willReturn(List.of(media));
            given(mediaRepository.findById(1L)).willReturn(Optional.of(media));

            Resource resource = mock(Resource.class);
            given(resource.getInputStream()).willReturn(new ByteArrayInputStream("dummy".getBytes()));
            given(fileStorageService.downloadFile("images/originals/abc-123.jpg")).willReturn(resource);

            scheduler.retryFailedDerivatives();

            verify(mediaRepository).save(media);
            assertThat(media.getRetryCount()).isEqualTo(1);
            assertThat(media.getLastAttemptAt()).isNotNull();

            verify(asyncFileProcessingService).generateDerivativesAsync(
                    eq(1L), eq("abc-123"), argThat(bytes -> new String(bytes).equals("dummy")));
        }

        @Test
        @DisplayName("원본 다운로드가 실패한 건은 재생성을 요청하지 않고, 다른 건 처리에는 영향을 주지 않는다")
        void continuesWhenDownloadFailsForOneCandidate() throws IOException {
            scheduler = new MediaReprocessingScheduler(
                    mediaRepository, fileStorageService, asyncFileProcessingService, immediateExecutor);

            Media failing = buildFailedMedia(1L, "images/originals/broken.jpg");
            Media healthy = buildFailedMedia(2L, "images/originals/ok.jpg");

            given(mediaRepository.findRetryCandidates(anyInt(), any(LocalDateTime.class), any(Pageable.class)))
                    .willReturn(List.of(failing, healthy));
            given(mediaRepository.findById(1L)).willReturn(Optional.of(failing));
            given(mediaRepository.findById(2L)).willReturn(Optional.of(healthy));

            given(fileStorageService.downloadFile("images/originals/broken.jpg"))
                    .willThrow(new IOException("S3 down"));
            Resource okResource = mock(Resource.class);
            given(okResource.getInputStream()).willReturn(new ByteArrayInputStream("ok".getBytes()));
            given(fileStorageService.downloadFile("images/originals/ok.jpg")).willReturn(okResource);

            assertThatCode(() -> scheduler.retryFailedDerivatives()).doesNotThrowAnyException();

            verify(asyncFileProcessingService, never()).generateDerivativesAsync(eq(1L), any(), any());
            verify(asyncFileProcessingService).generateDerivativesAsync(eq(2L), eq("ok"), any());
        }

        @Test
        @DisplayName("스케줄 조회와 실제 재시도 사이에 삭제된 건은 예외 없이 건너뛴다")
        void skipsWhenMediaDeletedConcurrently() {
            scheduler = new MediaReprocessingScheduler(
                    mediaRepository, fileStorageService, asyncFileProcessingService, immediateExecutor);
            Media ghost = buildFailedMedia(99L, "images/originals/ghost.jpg");

            given(mediaRepository.findRetryCandidates(anyInt(), any(LocalDateTime.class), any(Pageable.class)))
                    .willReturn(List.of(ghost));
            given(mediaRepository.findById(99L)).willReturn(Optional.empty());

            assertThatCode(() -> scheduler.retryFailedDerivatives()).doesNotThrowAnyException();

            verifyNoInteractions(fileStorageService, asyncFileProcessingService);
        }
    }
}
