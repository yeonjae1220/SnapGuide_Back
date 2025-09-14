package yeonjae.snapguide.service.fileStorageService;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3FileStorageService implements FileStorageService{

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;


    @Override
    public UploadFileDto uploadFile(MultipartFile file) throws IOException {
        // 파일 기본 정보 설정
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isEmpty()) throw new IOException("파일 이름이 없습니다.");

        String extension = getExtension(originalFileName); // 예: "jpg", "heic"
        String baseFileName = UUID.randomUUID().toString();

        // 2. S3에 저장될 파일 경로 설정
        String originalKey = "images/originals/" + baseFileName + "." + extension;
        String webOriginalKey = "images/web/" + baseFileName + ".jpg";
        String thumbnailKey = "images/thumbnails/" + baseFileName + ".jpg";

        // 3. 메타데이터 생성 (파일 타입 지정)
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        // 4. 원본 파일 S3에 업로드
        // multipartFile.getInputStream()을 그대로 사용하여 로컬 저장 없이 바로 S3로 전송
        amazonS3.putObject(bucketName, originalKey, file.getInputStream(), metadata);
        String originalFileUrl = amazonS3.getUrl(bucketName, originalKey).toString();


        // 5. 웹용 고화질 JPG 생성 및 업로드 (in-memory 처리)
        ByteArrayOutputStream webOriginalOutputStream = new ByteArrayOutputStream();
        Thumbnails.of(file.getInputStream())
                .scale(1.0)
                .outputFormat("jpg")
                .toOutputStream(webOriginalOutputStream);

        byte[] webOriginalImageBytes = webOriginalOutputStream.toByteArray();
        ObjectMetadata webOriginalMetadata = createMetadata("image/jpeg", webOriginalImageBytes.length);
        InputStream webOriginalInputStream = new ByteArrayInputStream(webOriginalImageBytes);

        amazonS3.putObject(bucketName, webOriginalKey, webOriginalInputStream, webOriginalMetadata);
        String webOriginalFileUrl = amazonS3.getUrl(bucketName, webOriginalKey).toString();


        // 6. 썸네일 생성 및 업로드 (in-memory 처리)
        ByteArrayOutputStream thumbnailOutputStream = new ByteArrayOutputStream();
        // 방금 만든 웹용 원본 이미지를 다시 읽어서 썸네일 생성
        Thumbnails.of(new ByteArrayInputStream(webOriginalImageBytes))
                .size(1080, 1080)
                .outputQuality(0.7)
                .toOutputStream(thumbnailOutputStream);

        byte[] thumbnailImageBytes = thumbnailOutputStream.toByteArray();
        ObjectMetadata thumbnailMetadata = createMetadata("image/jpeg", thumbnailImageBytes.length);
        InputStream thumbnailInputStream = new ByteArrayInputStream(thumbnailImageBytes);

        amazonS3.putObject(bucketName, thumbnailKey, thumbnailInputStream, thumbnailMetadata);
        String thumbnailFileUrl = amazonS3.getUrl(bucketName, thumbnailKey).toString();


        // 7. 결과 반환 (필요에 따라 DTO 생성)
        // return new UploadResult(originalFileUrl, webOriginalFileUrl, thumbnailFileUrl);
        // return webOriginalFileUrl.
        return UploadFileDto.builder()
                .imageBytes(webOriginalImageBytes)
                .originalDir(webOriginalFileUrl)
                .thumbnailDir(thumbnailFileUrl)
                .build();

    }

    @Override
    public Resource downloadFile(String filePath) throws IOException {
        return null;
    }

    @Override
    public void deleteFile(String filePath) throws IOException {

    }

    @Override
    public String generatePublicUrl(String filePath) {
        return null;
    }



    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot + 1).toLowerCase();
    }

    // 메타데이터 생성을 위한 헬퍼 메서드
    private ObjectMetadata createMetadata(String contentType, long contentLength) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(contentLength);
        return metadata;
    }

    // 업로드 결과를 담을 DTO
    public static class UploadResult {
        public final String originalUrl;
        public final String webUrl;
        public final String thumbnailUrl;

        public UploadResult(String originalUrl, String webUrl, String thumbnailUrl) {
            this.originalUrl = originalUrl;
            this.webUrl = webUrl;
            this.thumbnailUrl = thumbnailUrl;
        }
    }
}
