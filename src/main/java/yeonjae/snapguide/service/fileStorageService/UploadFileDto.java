package yeonjae.snapguide.service.fileStorageService;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadFileDto {
    byte[] imageBytes;
    String originalDir;
    String thumbnailDir;

}
