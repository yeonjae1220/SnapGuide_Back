package yeonjae.snapguide.service.fileStorageService;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadFileDto {
    byte[] originalFileBytes;
    String originalDir;
    String originalKey;      // S3 key 또는 로컬 경로
    String baseFileName;     // UUID 기반 파일명 (비동기 처리용)
    String webDir;
    String thumbnailDir;
}
