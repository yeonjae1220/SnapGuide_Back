package yeonjae.snapguide.domain.media;

/**
 * Media Entity ↔ MediaDto 변환을 담당하는 Mapper
 * LocationMapper 패턴을 따라 일관성 있는 변환 로직 제공
 */
public class MediaMapper {

    /**
     * MediaDto → Media Entity 변환
     * @param dto MediaDto
     * @return Media Entity (연관관계는 별도로 설정 필요)
     */
    public static Media toEntity(MediaDto dto) {
        return Media.builder()
                .mediaName(dto.getFileName())
                .mediaUrl(dto.getUrl())
                .build();
    }

    /**
     * Media Entity → MediaDto 변환
     * Lazy Loading 방지를 위해 기본 필드만 변환
     * @param entity Media Entity
     * @return MediaDto
     */
    public static MediaDto toDto(Media entity) {
        if (entity == null) {
            return null;
        }

        return new MediaDto(
                entity.getMediaName(),
                entity.getMediaUrl()
        );
    }

    /**
     * Media Entity → MediaDto 변환 (상세 정보 포함)
     * @param entity Media Entity
     * @return MediaDto with additional fields
     */
    public static MediaDto toDtoWithDetails(Media entity) {
        if (entity == null) {
            return null;
        }

        MediaDto dto = new MediaDto(
                entity.getMediaName(),
                entity.getMediaUrl()
        );
        // 추후 필요 시 fileSize, createdAt 등 추가 필드 설정
        return dto;
    }
}
