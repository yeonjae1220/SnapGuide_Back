package yeonjae.snapguide.service.fileStorageService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import yeonjae.snapguide.service.fileStorageService.fileConverter.HeicConverter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import net.coobird.thumbnailator.Thumbnails;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private static final int THUMBNAIL_SIZE = 1080;
    private static final double THUMBNAIL_QUALITY = 0.7;

    @Value("${storage.local.base-dir}")
    private String uploadBasePath;

    private Path uploadOriginalDir;
    private Path uploadThumbnailDir;

    private final HeicConverter heicConverter = new HeicConverter();

    @PostConstruct
    private void initPaths() {
        uploadOriginalDir = Paths.get(uploadBasePath).resolve("originals");
        uploadThumbnailDir = Paths.get(uploadBasePath);
    }

    /**
     * 원본 파일만 로컬에 저장 (동기 - 빠른 응답용)
     * rawBytes를 외부에서 받아 이중 읽기를 방지
     */
    @Override
    public UploadFileDto uploadOriginalOnly(byte[] rawBytes, String originalFilename) throws IOException {
        log.info("[Fast Upload-Local] Starting for: {}", originalFilename);
        long startTime = System.currentTimeMillis();

        String baseFileName = UUID.randomUUID().toString();
        String originalFileNameWithExt = baseFileName + ".jpg";

        byte[] originalJpgBytes = convertToJpg(rawBytes);

        Path originalPath = uploadOriginalDir.resolve(originalFileNameWithExt);
        Files.createDirectories(originalPath.getParent());
        Files.copy(new ByteArrayInputStream(originalJpgBytes), originalPath, StandardCopyOption.REPLACE_EXISTING);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[Fast Upload-Local] Completed in {}ms. Original: {}", elapsed, originalPath);

        return UploadFileDto.builder()
                .originalFileBytes(originalJpgBytes)
                .originalDir(originalPath.toString())
                .originalKey(originalPath.toString())
                .baseFileName(baseFileName)
                .thumbnailDir(null)
                .build();
    }

    /**
     * @deprecated 성능 이슈로 uploadOriginalOnly + generateDerivativesAsync 조합 권장
     */
    @Override
    @Deprecated
    public UploadFileDto uploadFile(MultipartFile multipartFile) throws IOException {
        String baseFileName = UUID.randomUUID().toString();
        String originalFileNameWithExt = baseFileName + ".jpg";
        String thumbnailFileNameWithExt = baseFileName + "_thumb.jpg";

        byte[] originalJpgBytes = convertToJpg(multipartFile.getBytes());

        // 2. 변환된 JPG 바이트 배열을 사용해 썸네일 생성
        ByteArrayOutputStream thumbnailOutputStream = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(originalJpgBytes))
                .size(THUMBNAIL_SIZE, THUMBNAIL_SIZE)
                .outputQuality(THUMBNAIL_QUALITY)
                .toOutputStream(thumbnailOutputStream);
        byte[] thumbnailBytes = thumbnailOutputStream.toByteArray();

        // 3. 결과물들을 로컬 디스크에 저장
        // 원본 저장
        Path originalPath = uploadOriginalDir.resolve(originalFileNameWithExt);
        Files.createDirectories(originalPath.getParent());
        Files.copy(new ByteArrayInputStream(originalJpgBytes), originalPath, StandardCopyOption.REPLACE_EXISTING);

        // 썸네일 저장
        Path thumbnailPath = uploadThumbnailDir.resolve(thumbnailFileNameWithExt);
        Files.createDirectories(thumbnailPath.getParent());
        Files.copy(new ByteArrayInputStream(thumbnailBytes), thumbnailPath, StandardCopyOption.REPLACE_EXISTING);

        return UploadFileDto.builder()
                .originalFileBytes(originalJpgBytes)
                .originalDir(originalPath.toString())
                .originalKey(originalPath.toString())
                .baseFileName(baseFileName)
                .thumbnailDir(thumbnailPath.toString())
                .build();
    }

    /**
     * 확장자 추출 (예: "jpg", "heic", "png")
     */
    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot + 1).toLowerCase();
    }

    private byte[] convertToJpg(byte[] imageBytes) throws IOException {
        String mimeType = FileTypeDetector.detectMimeType(new ByteArrayInputStream(imageBytes));
        if ("image/heic".equalsIgnoreCase(mimeType) || "image/heif".equalsIgnoreCase(mimeType)) {
            return heicConverter.convertToJpgBytes(new ByteArrayInputStream(imageBytes));
        }
        ByteArrayOutputStream jpgOutputStream = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(imageBytes))
                .scale(1.0)
                .outputFormat("jpg")
                .toOutputStream(jpgOutputStream);
        return jpgOutputStream.toByteArray();
    }


    @Override
    public Resource downloadFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        Resource resource = new UrlResource(path.toUri());
        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new FileNotFoundException("파일을 읽을 수 없습니다: " + filePath);
        }
    }

    @Override
    public void deleteFile(String filePath) throws IOException {
        Files.deleteIfExists(Paths.get(filePath));
    }

    @Override
    public String generatePublicUrl(String filePath) {
        return "/media/files/" + Paths.get(filePath).getFileName().toString(); // 예: uuid_img.jpg
    }

}