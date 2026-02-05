package yeonjae.snapguide.service.fileStorageService;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface FileStorageService {
    /**
     * 파일 업로드 (동기 - 모든 파일 생성 후 반환)
     * @deprecated 성능 이슈로 uploadOriginalOnly + generateDerivativesAsync 조합 권장
     */
    UploadFileDto uploadFile(MultipartFile file) throws IOException;

    /**
     * 원본 파일만 업로드 (동기 - 빠른 응답)
     * 썸네일/웹용 파일은 generateDerivativesAsync()로 비동기 생성
     */
    default UploadFileDto uploadOriginalOnly(MultipartFile file) throws IOException {
        // 기본 구현은 기존 uploadFile() 호출 (하위 호환성)
        return uploadFile(file);
    }

    /**
     * 파생 파일(썸네일, 웹용) 비동기 생성
     * @param mediaId Media 엔티티 ID (완료 후 업데이트용)
     * @param originalKey 원본 파일 키/경로
     * @param originalBytes 원본 파일 바이트 (이미지 변환용)
     */
    default void generateDerivativesAsync(Long mediaId, String originalKey, byte[] originalBytes) {
        // 기본 구현은 아무것도 안 함 (하위 호환성)
    }

    Resource downloadFile(String filePath) throws IOException;

    void deleteFile(String filePath) throws IOException;

    /**
     * 파일 접근용 URL 생성
     */
    String generatePublicUrl(String filePath);
}

/**
 * TODO : 추후 S3 저장용 구현체도 만들 예정, 만들때 config파일 만들어서 빈 넣어서 yml파일 기반으로 선택할 수 있게 (노션 로컬에 저장하다 s3로 전환 방법에 정리)
 */