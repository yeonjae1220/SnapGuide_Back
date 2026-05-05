package yeonjae.snapguide.service.mediaSerivce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import yeonjae.snapguide.domain.media.Media;
import yeonjae.snapguide.domain.media.MediaDto;
import yeonjae.snapguide.domain.media.MediaMapper;
import yeonjae.snapguide.repository.mediaRepository.MediaRepository;
import yeonjae.snapguide.service.fileStorageService.AsyncFileProcessingService;
import yeonjae.snapguide.service.mediaSerivce.MediaSingleFileProcessor.FileProcessResult;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final AsyncFileProcessingService asyncFileProcessingService;
    private final MediaRepository mediaRepository;
    private final MediaSingleFileProcessor singleFileProcessor;

    @Qualifier("uploadProcessingExecutor")
    private final Executor uploadProcessingExecutor;

    /**
     * 여러 파일을 병렬로 업로드한다.
     *
     * - 각 파일은 독립된 REQUIRES_NEW 트랜잭션(MediaSingleFileProcessor)으로 처리되므로
     *   한 파일 실패가 다른 파일 저장에 영향을 주지 않는다.
     * - 비동기 파생 파일 생성(썸네일/웹용)은 각 파일 처리 완료 즉시 CompletableFuture 내부에서
     *   dispatch하여 originalBytes 참조를 가능한 빨리 해제한다.
     * - NOT_SUPPORTED: 클래스 레벨 @Transactional 이 열어 놓는 빈 outer 트랜잭션을 방지.
     *   실제 DB 작업은 모두 별도 스레드의 REQUIRES_NEW 트랜잭션에서 수행된다.
     *
     * @param files       업로드 파일 목록
     * @param fallbackLat GPS EXIF 없을 때 사용할 위도 (nullable)
     * @param fallbackLng GPS EXIF 없을 때 사용할 경도 (nullable)
     * @return 저장 성공 Media 목록 + 스킵된 파일명 목록
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BatchUploadResult saveAllAndGet(List<MultipartFile> files, Double fallbackLat, Double fallbackLng) {
        List<String> skippedFiles = new CopyOnWriteArrayList<>();

        List<CompletableFuture<Optional<Media>>> futures = files.stream()
                .map(file -> CompletableFuture.supplyAsync(() -> {
                    try {
                        FileProcessResult result =
                                singleFileProcessor.processFile(file, fallbackLat, fallbackLng);

                        // 파일 처리 즉시 async dispatch (Problem E: 바이트 참조 조기 해제)
                        asyncFileProcessingService.generateDerivativesAsync(
                                result.media().getId(),
                                result.baseFileName(),
                                result.originalBytes()
                        );

                        return Optional.of(result.media());
                    } catch (Exception e) {
                        String fileName = (file.getOriginalFilename() != null)
                                ? file.getOriginalFilename() : "unknown";
                        log.warn("[Upload] Skipping '{}': {}", fileName, e.getMessage());
                        skippedFiles.add(fileName);
                        return Optional.<Media>empty();
                    }
                }, uploadProcessingExecutor))
                .toList();

        // 모든 파일 처리 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<Media> savedMedia = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(Optional::stream)
                .toList();

        if (!skippedFiles.isEmpty()) {
            log.warn("[Upload] Batch complete — saved: {}/{}, skipped: {}",
                    savedMedia.size(), files.size(), skippedFiles);
        } else {
            log.info("[Upload] Batch complete — all {} file(s) saved successfully", savedMedia.size());
        }

        return new BatchUploadResult(savedMedia, List.copyOf(skippedFiles));
    }

    /**
     * 위치 fallback 없이 업로드 (파일명만 반환).
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BatchUploadResult saveAllAndGet(List<MultipartFile> files) {
        return saveAllAndGet(files, null, null);
    }

    /**
     * 업로드 후 Media ID 목록만 반환 (MediaController 호환용).
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<Long> saveAll(List<MultipartFile> files) {
        return saveAllAndGet(files, null, null).savedMedia().stream()
                .map(Media::getId)
                .toList();
    }

    /**
     * 동기 업로드 (레거시 호환용) — 내부적으로 비동기 방식으로 처리됨
     *
     * @deprecated 성능 이슈로 saveAll() 사용 권장
     */
    @Deprecated
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<Long> saveAllSync(List<MultipartFile> files) {
        return saveAll(files);
    }

    /**
     * 모든 Media를 DTO로 반환
     */
    public List<MediaDto> getAllMedia() {
        return mediaRepository.findAll()
                .stream()
                .map(MediaMapper::toDto)
                .toList();
    }

    public String getPublicUrl(File savedFile) {
        return "/" + savedFile.getName();
    }

    /**
     * Guide 저장 시 대표 locationId 조회 (첫 번째 Media 기준)
     */
    public Long getOneLocationId(List<Long> mediaIds) {
        List<Long> locationIds = mediaRepository.findFirstLocationIdByMediaIds(mediaIds, PageRequest.of(0, 1));
        return locationIds.stream().findFirst().orElse(null);
    }
}
