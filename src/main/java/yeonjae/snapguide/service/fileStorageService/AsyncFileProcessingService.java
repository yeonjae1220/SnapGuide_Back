package yeonjae.snapguide.service.fileStorageService;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.repository.mediaRepository.MediaRepository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@Slf4j
public class AsyncFileProcessingService {

    private static final int THUMBNAIL_SIZE = 1080;
    private static final double THUMBNAIL_QUALITY = 0.7;

    private final MediaRepository mediaRepository;
    private final AmazonS3 amazonS3;

    @Autowired
    public AsyncFileProcessingService(
            MediaRepository mediaRepository,
            @Autowired(required = false) AmazonS3 amazonS3) {
        this.mediaRepository = mediaRepository;
        this.amazonS3 = amazonS3;
    }

    @Value("${cloud.aws.s3.bucket:}")
    private String bucketName;

    @Value("${storage.local.base-dir:uploads}")
    private String uploadBasePath;

    @Value("${storage.type:local}")
    private String storageType;

    /**
     * 비동기 파생 파일 생성 (트랜잭션 없음 - DB 커넥션 점유 최소화).
     * 이미지 변환/업로드 중에는 DB 커넥션을 사용하지 않는다.
     * 완료/실패 시 DB 업데이트는 별도 @Transactional 메서드로 처리.
     *
     * 실패 시 Media.processingStatus 를 FAILED 로 기록하여 추후 재처리 가능하게 한다.
     */
    @Async("fileProcessingExecutor")
    public void generateDerivativesAsync(Long mediaId, String baseFileName, byte[] originalBytes) {
        log.info("[Async] Starting derivative generation for mediaId: {}", mediaId);
        long startTime = System.currentTimeMillis();

        try {
            if ("s3".equals(storageType)) {
                generateS3Derivatives(mediaId, baseFileName, originalBytes);
            } else {
                generateLocalDerivatives(mediaId, baseFileName, originalBytes);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[Async] Derivative generation completed for mediaId: {} in {}ms", mediaId, elapsed);

        } catch (Exception e) {
            log.error("[Async] Derivative generation FAILED for mediaId: {}", mediaId, e);
            markProcessingFailed(mediaId);
        }
    }

    private void generateS3Derivatives(Long mediaId, String baseFileName, byte[] originalBytes) throws Exception {
        String webKey = "images/web/" + baseFileName + ".jpg";
        String thumbnailKey = "images/thumbnails/" + baseFileName + ".jpg";

        // 1. 웹용 JPG 생성
        log.info("[Async-S3] Creating web JPG for mediaId: {}", mediaId);
        ByteArrayOutputStream webOutputStream = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(originalBytes))
                .scale(1.0)
                .outputFormat("jpg")
                .toOutputStream(webOutputStream);
        byte[] webBytes = webOutputStream.toByteArray();

        // 2. 썸네일 생성
        log.info("[Async-S3] Creating thumbnail for mediaId: {}", mediaId);
        ByteArrayOutputStream thumbOutputStream = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(webBytes))
                .size(THUMBNAIL_SIZE, THUMBNAIL_SIZE)
                .outputQuality(THUMBNAIL_QUALITY)
                .toOutputStream(thumbOutputStream);
        byte[] thumbBytes = thumbOutputStream.toByteArray();

        // 3. S3 업로드
        log.info("[Async-S3] Uploading web JPG to S3 for mediaId: {}", mediaId);
        ObjectMetadata webMeta = createMetadata("image/jpeg", webBytes.length);
        amazonS3.putObject(bucketName, webKey, new ByteArrayInputStream(webBytes), webMeta);
        String webUrl = amazonS3.getUrl(bucketName, webKey).toString();

        log.info("[Async-S3] Uploading thumbnail to S3 for mediaId: {}", mediaId);
        ObjectMetadata thumbMeta = createMetadata("image/jpeg", thumbBytes.length);
        amazonS3.putObject(bucketName, thumbnailKey, new ByteArrayInputStream(thumbBytes), thumbMeta);
        String thumbUrl = amazonS3.getUrl(bucketName, thumbnailKey).toString();

        // 4. Media 엔티티 업데이트 (COMPLETED)
        markProcessingCompleted(mediaId, webUrl, thumbUrl, webKey, thumbnailKey);
    }

    private void generateLocalDerivatives(Long mediaId, String baseFileName, byte[] originalBytes) throws Exception {
        Path thumbnailDir = Paths.get(uploadBasePath);
        String thumbnailFileName = baseFileName + "_thumb.jpg";

        // 1. 썸네일 생성
        log.info("[Async-Local] Creating thumbnail for mediaId: {}", mediaId);
        ByteArrayOutputStream thumbOutputStream = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(originalBytes))
                .size(THUMBNAIL_SIZE, THUMBNAIL_SIZE)
                .outputQuality(THUMBNAIL_QUALITY)
                .toOutputStream(thumbOutputStream);
        byte[] thumbBytes = thumbOutputStream.toByteArray();

        // 2. 로컬 저장
        Path thumbnailPath = thumbnailDir.resolve(thumbnailFileName);
        Files.createDirectories(thumbnailPath.getParent());
        Files.copy(new ByteArrayInputStream(thumbBytes), thumbnailPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("[Async-Local] Thumbnail saved to: {}", thumbnailPath);

        // 3. Media 엔티티 업데이트 (로컬은 thumbnailKey만, COMPLETED)
        markProcessingCompleted(mediaId, null, null, null, thumbnailPath.toString(),
                "/media/files/" + thumbnailFileName);
    }

    /**
     * 비동기 처리 완료: webKey + thumbnailKey 업데이트, processingStatus = COMPLETED.
     * S3 용 — webKey, thumbnailKey 모두 존재.
     */
    @Transactional
    public void markProcessingCompleted(Long mediaId,
                                        String webUrl, String thumbUrl,
                                        String webKey, String thumbnailKey) {
        String publicUrl = "/media/files/" + extractFileName(webUrl != null ? webUrl : "");
        markProcessingCompleted(mediaId, webUrl, thumbUrl, webKey, thumbnailKey, publicUrl);
    }

    /**
     * 비동기 처리 완료: thumbnailKey 업데이트, processingStatus = COMPLETED.
     * 로컬 / S3 공용 — mediaUrl을 명시적으로 지정.
     */
    @Transactional
    public void markProcessingCompleted(Long mediaId,
                                        String webUrl, String thumbUrl,
                                        String webKey, String thumbnailKey,
                                        String mediaUrl) {
        mediaRepository.findById(mediaId).ifPresent(media -> {
            media.markProcessingCompleted(webKey, thumbnailKey, mediaUrl);
            mediaRepository.save(media);
            log.info("[Async] Media {} marked COMPLETED: web={}, thumb={}", mediaId, webKey, thumbnailKey);
        });
    }

    /**
     * 비동기 처리 실패: processingStatus = FAILED 로 기록.
     * webKey / thumbnailKey 는 null 유지 — 추후 재처리 시 식별 가능.
     */
    @Transactional
    public void markProcessingFailed(Long mediaId) {
        mediaRepository.findById(mediaId).ifPresent(media -> {
            media.markProcessingFailed();
            mediaRepository.save(media);
            log.error("[Async] Media {} marked as FAILED — derivative generation unsuccessful", mediaId);
        });
    }

    private String extractFileName(String url) {
        if (url == null || url.isEmpty()) return "";
        return Paths.get(url).getFileName().toString();
    }

    private ObjectMetadata createMetadata(String contentType, long contentLength) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(contentLength);
        return metadata;
    }
}
