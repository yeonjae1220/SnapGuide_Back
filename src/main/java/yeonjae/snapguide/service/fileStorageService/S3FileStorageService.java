package yeonjae.snapguide.service.fileStorageService;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
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

    /**
     * Presigned URL을 생성하는 메서드
     * @param filename 'images/web/' 경로를 제외한 순수 파일 이름 (예: uuid.jpg)
     * @return 생성된 Presigned URL 문자열
     */
    public String generatePresignedUrl(String filename) {
        // S3에 저장된 파일의 전체 경로 (Key)를 지정합니다.
        // '/media/files/' 요청은 일반적으로 웹용 이미지를 의미하므로 'images/web/' 경로를 사용합니다.
        String objectKey = "images/web/" + filename;

        // URL의 유효시간을 설정합니다. (예: 10분)
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * 10; // 10분
        expiration.setTime(expTimeMillis);

        try {
            // S3 객체가 실제로 존재하는지 먼저 확인합니다.
            if (!amazonS3.doesObjectExist(bucketName, objectKey)) {
                log.warn("S3에 존재하지 않는 파일에 대한 URL 생성 시도: {}", objectKey);
                return null; // 파일이 없으면 null 반환
            }

            // Presigned URL 생성 요청을 만듭니다.
            GeneratePresignedUrlRequest generatePresignedUrlRequest =
                    new GeneratePresignedUrlRequest(bucketName, objectKey)
                            .withMethod(HttpMethod.GET)
                            .withExpiration(expiration);

            // URL 생성
            URL url = amazonS3.generatePresignedUrl(generatePresignedUrlRequest);
            return url.toString();

        } catch (Exception e) {
            log.error("Presigned URL 생성 중 오류 발생: {}", e.getMessage());
            return null;
        }
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


}
