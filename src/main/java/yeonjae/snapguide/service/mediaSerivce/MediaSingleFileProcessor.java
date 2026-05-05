package yeonjae.snapguide.service.mediaSerivce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.domain.media.Media;
import yeonjae.snapguide.domain.media.ProcessingStatus;
import yeonjae.snapguide.domain.mediaMetaData.MediaMetaData;
import yeonjae.snapguide.repository.mediaRepository.MediaRepository;
import yeonjae.snapguide.service.fileStorageService.FileStorageService;
import yeonjae.snapguide.service.fileStorageService.UploadFileDto;
import yeonjae.snapguide.service.locationSerivce.LocationService;
import yeonjae.snapguide.service.mediaMetaDataSerivce.MediaMetaDataService;

import java.io.IOException;

/**
 * 파일 1개 처리를 독립된 REQUIRES_NEW 트랜잭션으로 캡슐화.
 *
 * MediaService.saveAllAndGet() 의 CompletableFuture 병렬 처리에서 호출되며,
 * 이 클래스를 별도 Spring bean 으로 분리함으로써 AOP 프록시가 정상 적용된다.
 * (같은 클래스 내 self-invocation 시 @Transactional 미적용 문제 해결)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaSingleFileProcessor {

    private final FileStorageService fileStorageService;
    private final MediaMetaDataService mediaMetaDataService;
    private final LocationService locationService;
    private final MediaRepository mediaRepository;

    /**
     * 파일 1개 처리 결과를 담는 record.
     * async dispatch 에 필요한 baseFileName 과 originalBytes 를 함께 반환하여
     * CompletableFuture lambda 내에서 즉시 비동기 작업을 제출할 수 있게 한다.
     */
    public record FileProcessResult(Media media, String baseFileName, byte[] originalBytes) {}

    /**
     * 파일 1개를 처리하여 Media 엔티티를 저장한다.
     *
     * REQUIRES_NEW: 호출 스레드의 트랜잭션과 독립적으로 새 트랜잭션을 열고,
     * 메서드 리턴 시 즉시 커밋한다. 따라서 이 파일 처리 실패 시 다른 파일에 영향 없음.
     *
     * @param file        업로드된 파일
     * @param fallbackLat GPS 없을 때 사용할 위도 (nullable)
     * @param fallbackLng GPS 없을 때 사용할 경도 (nullable)
     * @return 저장된 Media 엔티티와 비동기 처리에 필요한 정보
     * @throws IOException 파일 읽기 실패 시
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FileProcessResult processFile(MultipartFile file, Double fallbackLat, Double fallbackLng) throws IOException {
        long startTime = System.currentTimeMillis();

        // Thumbnailator 재인코딩 시 GPS EXIF 제거 → 변환 전 먼저 읽어둠
        byte[] rawBytes = file.getBytes();

        // 1. 원본 파일 업로드 (동기)
        UploadFileDto savedFile = fileStorageService.uploadOriginalOnly(rawBytes, file.getOriginalFilename());

        // 2. EXIF 메타데이터 추출 + 저장
        MediaMetaData metaData = mediaMetaDataService.extractAndSave(rawBytes);

        // 3. 위치 정보: GPS EXIF → 없으면 브라우저 좌표 fallback
        Location location = locationService.extractAndResolveLocation(rawBytes)
                .orElseGet(() -> {
                    if (fallbackLat != null && fallbackLng != null) {
                        try {
                            return locationService.saveLocation(fallbackLat, fallbackLng);
                        } catch (Exception e) {
                            log.warn("[Upload] Fallback reverse geocoding failed ({}, {}): {}",
                                    fallbackLat, fallbackLng, e.getMessage());
                            return null;
                        }
                    }
                    return null;
                });

        // 4. Media 엔티티 저장 (processingStatus = PENDING, webKey/thumbnailKey 는 비동기 완료 후 업데이트)
        String tempUrl = "/media/files/" + savedFile.getBaseFileName() + ".jpg";
        Media media = Media.builder()
                .mediaName(file.getOriginalFilename())
                .mediaUrl(tempUrl)
                .originalKey(savedFile.getOriginalKey())
                .webKey(null)
                .thumbnailKey(null)
                .processingStatus(ProcessingStatus.PENDING)
                .fileSize(file.getSize())
                .build();

        media.assignMedia(metaData, location);
        mediaRepository.save(media);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[Upload] Media {} saved in {}ms (async derivatives pending)", media.getId(), elapsed);

        return new FileProcessResult(media, savedFile.getBaseFileName(), savedFile.getOriginalFileBytes());
    }
}
