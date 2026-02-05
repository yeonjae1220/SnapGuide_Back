package yeonjae.snapguide.service.fileStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class LocalFileStorageService implements FileStorageService{

    /**
     * System.getProperty("user.dir")는 JVM이 실행 중인 현재 디렉토리의 절대 경로
     */
//    private final String uploadPath = System.getProperty("user.dir") + "/uploads";


//    private final Path uploadDir = Paths.get("uploads");
    // 변경: 절대 경로 사용
    private final Path uploadOriginalDir = Paths.get("/Users/kim-yeonjae/Desktop/Study/snapguide/uploads/originals");
    private final Path uploadThumbnailDir = Paths.get("/Users/kim-yeonjae/Desktop/Study/snapguide/uploads");
    /**
     * 이 주소가 더 보기 편하네
     *      private final String uploadOriginalDir = "C:/uploads/originals";
     *     private final String uploadThumbnailDir = "C:/uploads/thumbnails";
     */
    private final HeicConverter heicConverter = new HeicConverter();

    /**
     * 원본 파일만 로컬에 저장 (동기 - 빠른 응답용)
     * 썸네일은 AsyncFileProcessingService.generateDerivativesAsync()로 비동기 처리
     */
    @Override
    public UploadFileDto uploadOriginalOnly(MultipartFile multipartFile) throws IOException {
        log.info("[Fast Upload-Local] Starting for: {}", multipartFile.getOriginalFilename());
        long startTime = System.currentTimeMillis();

        String baseFileName = UUID.randomUUID().toString();
        String originalFileNameWithExt = baseFileName + ".jpg";

        // 1. 이미지를 JPG로 변환
        byte[] originalJpgBytes = convertToJpg(multipartFile);

        // 2. 원본만 저장 (썸네일은 비동기로 나중에)
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
                .thumbnailDir(null)  // 아직 생성 안 됨
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

        // 1. 이미지를 메모리에서 고화질 JPG 바이트 배열로 변환
        byte[] originalJpgBytes = convertToJpg(multipartFile);

        // 2. 변환된 JPG 바이트 배열을 사용해 썸네일 생성
        ByteArrayOutputStream thumbnailOutputStream = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(originalJpgBytes))
                .size(1080, 1080)
                .outputQuality(0.7)
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

    private byte[] convertToJpg(MultipartFile multipartFile) throws IOException {
        // 1. 스트림을 딱 한 번만 읽어 byte[] 배열에 저장합니다. (매우 중요)
        byte[] imageBytes = multipartFile.getBytes();

        // 2. Tika를 사용해 실제 파일 형식을 감지합니다.
        String mimeType = FileTypeDetector.detectMimeType(new ByteArrayInputStream(imageBytes));

        // 3. 파일 확장자가 아닌, MIME 타입을 기준으로 분기합니다.
        // HEIC/HEIF 형식은 "image/heic", "image/heif" MIME 타입을 가집니다.
        if ("image/heic".equalsIgnoreCase(mimeType) || "image/heif".equalsIgnoreCase(mimeType)) {
            // heicConverter는 새로운 스트림을 받아 처리합니다.
            return heicConverter.convertToJpgBytes(new ByteArrayInputStream(imageBytes));
        }

        // 4. 그 외 이미지(JPG, PNG 등)는 Thumbnails를 이용해 JPG로 통일합니다.
        ByteArrayOutputStream jpgOutputStream = new ByteArrayOutputStream();
        // Thumbnails도 새로운 스트림을 받아 처리합니다.
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


// TODO : 썸네일용 압축 파일도 처리 해야함