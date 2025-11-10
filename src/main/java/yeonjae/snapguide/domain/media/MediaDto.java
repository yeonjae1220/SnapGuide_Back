package yeonjae.snapguide.domain.media;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Media Entity를 표현하는 DTO
 * Lazy Loading 이슈를 방지하고 필요한 데이터만 전달
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MediaDto {
    private String fileName;
    private String url;

    /**
     * @deprecated MediaMapper.toDto()를 사용하세요
     * 하위 호환성을 위해 유지되지만 내부적으로 MediaMapper 위임
     */
    @Deprecated
    public static MediaDto fromEntity(Media media) {
        return MediaMapper.toDto(media);
    }
}
