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
    String thumbnailDir;

}
