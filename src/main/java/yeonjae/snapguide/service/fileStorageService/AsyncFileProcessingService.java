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

    private final MediaRepository mediaRepository;

    // S3용 (optional - Local 환경에서는 null)
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
     * 비동기 파생 파일 생성 (트랜잭션 없음 - DB 커넥션 점유 최소화)
     * 이미지 변환/업로드 중에는 DB 커넥션을 사용하지 않음
     * DB 업데이트는 별도 트랜잭션으로 처리
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
            log.error("[Async] Failed to generate derivatives for mediaId: {}", mediaId, e);
        }
    }

    private void generateS3Derivatives(Long mediaId, String baseFileName, byte[] originalBytes) throws Exception {
        String webKey = "images/web/" + baseFileName + ".jpg";
        String thumbnailKey = "images/thumbnails/" + baseFileName + ".jpg";

        // 1. 웹용 JPG 생성
        log.info("[Async-S3] Creating web JPG...");
        ByteArrayOutputStream webOutputStream = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(originalBytes))
                .scale(1.0)
                .outputFormat("jpg")
                .toOutputStream(webOutputStream);
        byte[] webBytes = webOutputStream.toByteArray();

        // 2. 썸네일 생성
        log.info("[Async-S3] Creating thumbnail...");
        ByteArrayOutputStream thumbOutputStream = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(webBytes))
                .size(1080, 1080)
                .outputQuality(0.7)
                .toOutputStream(thumbOutputStream);
        byte[] thumbBytes = thumbOutputStream.toByteArray();

        // 3. S3 업로드
        log.info("[Async-S3] Uploading web JPG to S3...");
        ObjectMetadata webMeta = createMetadata("image/jpeg", webBytes.length);
        amazonS3.putObject(bucketName, webKey, new ByteArrayInputStream(webBytes), webMeta);
        String webUrl = amazonS3.getUrl(bucketName, webKey).toString();

        log.info("[Async-S3] Uploading thumbnail to S3...");
        ObjectMetadata thumbMeta = createMetadata("image/jpeg", thumbBytes.length);
        amazonS3.putObject(bucketName, thumbnailKey, new ByteArrayInputStream(thumbBytes), thumbMeta);
        String thumbUrl = amazonS3.getUrl(bucketName, thumbnailKey).toString();

        // 4. Media 엔티티 업데이트
        updateMediaUrls(mediaId, webUrl, thumbUrl, webKey, thumbnailKey);
    }

    private void generateLocalDerivatives(Long mediaId, String baseFileName, byte[] originalBytes) throws Exception {
        Path thumbnailDir = Paths.get(uploadBasePath);
        String thumbnailFileName = baseFileName + "_thumb.jpg";

        // 1. 썸네일 생성
        log.info("[Async-Local] Creating thumbnail...");
        ByteArrayOutputStream thumbOutputStream = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(originalBytes))
                .size(1080, 1080)
                .outputQuality(0.7)
                .toOutputStream(thumbOutputStream);
        byte[] thumbBytes = thumbOutputStream.toByteArray();

        // 2. 로컬 저장
        Path thumbnailPath = thumbnailDir.resolve(thumbnailFileName);
        Files.createDirectories(thumbnailPath.getParent());
        Files.copy(new ByteArrayInputStream(thumbBytes), thumbnailPath, StandardCopyOption.REPLACE_EXISTING);

        log.info("[Async-Local] Thumbnail saved to: {}", thumbnailPath);

        // 3. Media 엔티티 업데이트 (로컬은 썸네일만)
        updateMediaThumbnail(mediaId, thumbnailPath.toString(), "/media/files/" + thumbnailFileName);
    }

    /**
     * DB 업데이트만 트랜잭션으로 처리 (커넥션 점유 최소화)
     */
    @Transactional
    public void updateMediaUrls(Long mediaId, String webUrl, String thumbUrl, String webKey, String thumbnailKey) {
        mediaRepository.findById(mediaId).ifPresent(media -> {
            media.updateDerivativeUrls(webKey, thumbnailKey, "/media/files/" + extractFileName(webUrl));
            mediaRepository.save(media);
            log.info("[Async] Media {} URLs updated: web={}, thumb={}", mediaId, webKey, thumbnailKey);
        });
    }

    /**
     * DB 업데이트만 트랜잭션으로 처리 (커넥션 점유 최소화)
     */
    @Transactional
    public void updateMediaThumbnail(Long mediaId, String thumbnailPath, String thumbnailUrl) {
        mediaRepository.findById(mediaId).ifPresent(media -> {
            media.updateThumbnailUrl(thumbnailPath, thumbnailUrl);
            mediaRepository.save(media);
            log.info("[Async] Media {} thumbnail updated: {}", mediaId, thumbnailUrl);
        });
    }

    private String extractFileName(String url) {
        return Paths.get(url).getFileName().toString();
    }

    private ObjectMetadata createMetadata(String contentType, long contentLength) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(contentLength);
        return metadata;
    }
}
