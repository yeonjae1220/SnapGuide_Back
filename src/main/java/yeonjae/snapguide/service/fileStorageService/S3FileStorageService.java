package yeonjae.snapguide.service.fileStorageService;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.tika.Tika;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.im4java.process.Pipe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    /**
     * 원본 파일만 S3에 업로드 (동기 - 빠른 응답용)
     * 웹용/썸네일은 AsyncFileProcessingService.generateDerivativesAsync()로 비동기 처리
     */
    @Override
    public UploadFileDto uploadOriginalOnly(MultipartFile file) throws IOException {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isEmpty()) {
            throw new IOException("파일 이름이 없습니다.");
        }

        log.info("[Fast Upload] Starting for: {}, Size: {} bytes", originalFileName, file.getSize());
        long startTime = System.currentTimeMillis();

        try {
            // 1. Tika로 실제 파일 타입 감지
            Tika tika = new Tika();
            String mimeType = tika.detect(file.getInputStream());

            byte[] fileBytes = file.getBytes();
            String extension = getExtension(originalFileName);

            // 2. HEIC/HEIF인 경우 JPG 변환 (원본 저장 전에 필요)
            if ("image/heic".equals(mimeType) || "image/heif".equals(mimeType)) {
                log.info("[Fast Upload] HEIC detected, converting...");
                fileBytes = convertHeicToJpg(fileBytes);
                extension = "jpg";
            }

            String baseFileName = UUID.randomUUID().toString();
            String originalKey = "images/originals/" + baseFileName + "." + extension;

            // 3. 원본 파일만 S3 업로드 (1회만!)
            ObjectMetadata metadata = createMetadata(file.getContentType(), fileBytes.length);
            amazonS3.putObject(bucketName, originalKey, new ByteArrayInputStream(fileBytes), metadata);
            String originalUrl = amazonS3.getUrl(bucketName, originalKey).toString();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[Fast Upload] Completed in {}ms. Original: {}", elapsed, originalKey);

            // 4. 원본 정보만 반환 (웹용/썸네일은 비동기로 나중에)
            return UploadFileDto.builder()
                    .originalFileBytes(fileBytes)
                    .originalDir(originalUrl)
                    .originalKey(originalKey)
                    .baseFileName(baseFileName)
                    .webDir(null)
                    .thumbnailDir(null)
                    .build();

        } catch (Exception e) {
            log.error("[Fast Upload] Failed for: {}", originalFileName, e);
            throw new IOException("파일 업로드 중 오류가 발생했습니다: " + originalFileName, e);
        }
    }

    /**
     * @deprecated 성능 이슈로 uploadOriginalOnly + generateDerivativesAsync 조합 권장
     */
    @Override
    @Transactional
    @Deprecated
    public UploadFileDto uploadFile(MultipartFile file) throws IOException {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isEmpty()) {
            throw new IOException("파일 이름이 없습니다.");
        }

        log.info("Starting file upload for: {}, Size: {} bytes, ContentType from client: {}",
                originalFileName, file.getSize(), file.getContentType());

        try {
            // Tika로 실제 파일 타입 감지
            Tika tika = new Tika();
            String mimeType = tika.detect(file.getInputStream());
            log.info("Detected MIME type by Tika: {}", mimeType);

            byte[] fileBytes = file.getBytes();
            String extension = getExtension(originalFileName);

            // HEIC/HEIF인 경우 ImageMagick으로 JPG 변환
            if ("image/heic".equals(mimeType) || "image/heif".equals(mimeType)) {
                log.info("HEIC/HEIF detected. Converting to JPG using ImageMagick...");
                fileBytes = convertHeicToJpg(fileBytes);
                log.info("Conversion successful. New size: {} bytes", fileBytes.length);
                extension = "jpg"; // 확장자를 jpg로 변경
            }

            String baseFileName = UUID.randomUUID().toString();

            // S3 경로 설정
            String originalKey = "images/originals/" + baseFileName + "." + getExtension(originalFileName); // 원본은 원래 확장자 유지
            String webOriginalKey = "images/web/" + baseFileName + ".jpg";
            String thumbnailKey = "images/thumbnails/" + baseFileName + ".jpg";

            // 1. 원본 파일 업로드 (사용자의 원본 파일을 그대로 저장)
            ObjectMetadata metadata = createMetadata(file.getContentType(), file.getSize());
            log.info("Step 1/5: Uploading original file to S3. Key: {}", originalKey);
            amazonS3.putObject(bucketName, originalKey, file.getInputStream(), metadata);
            String originalFileUrl = amazonS3.getUrl(bucketName, originalKey).toString();
            log.info(" -> Original file uploaded successfully.");

            // 2. 웹용 고화질 JPG 생성 (변환되었거나 원래 이미지인 바이트 사용)
            log.info("Step 2/5: Creating web-friendly JPG version...");
            ByteArrayOutputStream webOriginalOutputStream = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(fileBytes))
                    .scale(1.0)
                    .outputFormat("jpg")
                    .toOutputStream(webOriginalOutputStream);
            byte[] webOriginalImageBytes = webOriginalOutputStream.toByteArray();

            if (webOriginalImageBytes == null || webOriginalImageBytes.length == 0) {
                log.error("Web-friendly JPG conversion resulted in an empty image.");
                throw new IOException("웹용 이미지 변환에 실패했습니다.");
            }
            log.info(" -> Web JPG created. Size: {} bytes", webOriginalImageBytes.length);

            // 3. 웹용 JPG 업로드
            ObjectMetadata webOriginalMetadata = createMetadata("image/jpeg", webOriginalImageBytes.length);
            log.info("Step 3/5: Uploading web-friendly JPG to S3. Key: {}", webOriginalKey);
            amazonS3.putObject(bucketName, webOriginalKey, new ByteArrayInputStream(webOriginalImageBytes), webOriginalMetadata);
            String webOriginalFileUrl = amazonS3.getUrl(bucketName, webOriginalKey).toString();
            log.info(" -> Web JPG uploaded successfully.");

            // 4. 썸네일 생성
            log.info("Step 4/5: Creating thumbnail version...");
            ByteArrayOutputStream thumbnailOutputStream = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(webOriginalImageBytes))
                    .size(1080, 1080)
                    .outputQuality(0.7)
                    .toOutputStream(thumbnailOutputStream);
            byte[] thumbnailImageBytes = thumbnailOutputStream.toByteArray();

            if (thumbnailImageBytes == null || thumbnailImageBytes.length == 0) {
                log.error("Thumbnail creation resulted in an empty image.");
                throw new IOException("썸네일 이미지 생성에 실패했습니다.");
            }
            log.info(" -> Thumbnail created. Size: {} bytes", thumbnailImageBytes.length);

            // 5. 썸네일 업로드
            ObjectMetadata thumbnailMetadata = createMetadata("image/jpeg", thumbnailImageBytes.length);
            log.info("Step 5/5: Uploading thumbnail to S3. Key: {}", thumbnailKey);
            amazonS3.putObject(bucketName, thumbnailKey, new ByteArrayInputStream(thumbnailImageBytes), thumbnailMetadata);
            String thumbnailFileUrl = amazonS3.getUrl(bucketName, thumbnailKey).toString();
            log.info(" -> Thumbnail uploaded successfully.");

            log.info("File upload process completed successfully for: {}", originalFileName);

            return UploadFileDto.builder()
                    .originalFileBytes(fileBytes) // 변환된 JPG가 아닌, 원본 파일 바이트를 담아 반환
                    .originalDir(originalFileUrl)
                    .webDir(webOriginalFileUrl)
                    .thumbnailDir(thumbnailFileUrl)
                    .build();

        } catch (Exception e) {
            log.error("File upload failed for: {}. Error: {}", originalFileName, e.getMessage(), e);
            throw new IOException("파일 업로드 중 오류가 발생했습니다: " + originalFileName, e);
        }
    }

    private byte[] convertHeicToJpg(byte[] heicBytes) {
        try {
            IMOperation op = new IMOperation();
            op.addImage("-"); // 입력: 표준 입력 (stdin)
            op.addImage("jpeg:-"); // 출력: 표준 출력 (stdout), 포맷: jpeg

            ConvertCmd convert = new ConvertCmd();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(heicBytes);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Pipe pipeIn = new Pipe(inputStream, null);
            Pipe pipeOut = new Pipe(null, outputStream);

            convert.setInputProvider(pipeIn);
            convert.setOutputConsumer(pipeOut);

            convert.run(op);

            inputStream.close();
            outputStream.close();

            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("ImageMagick HEIC to JPG conversion failed", e);
            throw new RuntimeException("이미지 변환에 실패했습니다.", e);
        }
    }

    public String generatePresignedUrl(String filename) {
        String objectKey = "images/web/" + filename;
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * 10; // 10분
        expiration.setTime(expTimeMillis);

        try {
            if (!amazonS3.doesObjectExist(bucketName, objectKey)) {
                log.warn("S3에 존재하지 않는 파일에 대한 URL 생성 시도: {}", objectKey);
                return null;
            }
            GeneratePresignedUrlRequest generatePresignedUrlRequest =
                    new GeneratePresignedUrlRequest(bucketName, objectKey)
                            .withMethod(HttpMethod.GET)
                            .withExpiration(expiration);
            URL url = amazonS3.generatePresignedUrl(generatePresignedUrlRequest);
            return url.toString();
        } catch (Exception e) {
            log.error("Presigned URL 생성 중 오류 발생: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public Resource downloadFile(String filePath) throws IOException {
        // TODO: S3에서 파일 다운로드 로직 구현
        return null;
    }

    @Override
    public void deleteFile(String filePath) throws IOException {
        if (filePath == null || filePath.isEmpty()) return;
        try {
            amazonS3.deleteObject(bucketName, filePath);
            log.info("S3 file deleted successfully. Key: {}", filePath);
        } catch (Exception e) {
            log.error("Failed to delete S3 file. Key: {}", filePath, e);
            throw new IOException("S3 파일 삭제 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public String generatePublicUrl(String filePath) {
        // TODO: S3 파일의 public URL 생성 로직 구현
        return null;
    }

    private String getExtension(String fileName) {
        try {
            return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        } catch (StringIndexOutOfBoundsException e) {
            return ""; // 확장자가 없는 경우
        }
    }

    private ObjectMetadata createMetadata(String contentType, long contentLength) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(contentLength);
        return metadata;
    }
}