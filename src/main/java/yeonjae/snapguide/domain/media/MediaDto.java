package yeonjae.snapguide.domain.media;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MediaDto {
    private String fileName;
    private String url;
    // size, createdAt, type 등을 추가해 확장
}
