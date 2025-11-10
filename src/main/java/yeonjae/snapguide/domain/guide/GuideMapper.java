package yeonjae.snapguide.domain.guide;

import yeonjae.snapguide.controller.guideController.guideDto.GuideResponseDto;
import yeonjae.snapguide.domain.media.MediaDto;
import yeonjae.snapguide.domain.media.MediaMapper;
import yeonjae.snapguide.domain.member.dto.MemberDto;
import yeonjae.snapguide.domain.member.MemberMapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Guide Entity ↔ DTO 변환을 담당하는 Mapper
 * LocationMapper 패턴을 따라 일관성 있는 변환 로직 제공
 */
public class GuideMapper {

    /**
     * Guide Entity → GuideResponseDto 변환
     * Lazy Loading 방지를 위해 트랜잭션 내에서 호출 필요
     *
     * @param entity Guide Entity
     * @param userHasLiked 사용자 좋아요 여부
     * @return GuideResponseDto
     */
    public static GuideResponseDto toResponseDto(Guide entity, boolean userHasLiked) {
        if (entity == null) {
            return null;
        }

        // Media 리스트 변환
        List<MediaDto> mediaDtos = entity.getMediaList()
                .stream()
                .map(MediaMapper::toDto)
                .collect(Collectors.toList());

        // Author 변환
        MemberDto authorDto = MemberMapper.toDto(entity.getAuthor());

        // Location 이름 추출 (null-safe)
        String locationName = entity.getLocation() != null
                ? entity.getLocation().getLocationName()
                : null;

        return GuideResponseDto.builder()
                .id(entity.getId())
                .tip(entity.getTip())
                .author(authorDto)
                .locationName(locationName)
                .media(mediaDtos)
                .likeCount(entity.getLikeCount())
                .userHasLiked(userHasLiked)
                .build();
    }

    /**
     * Guide Entity → GuideResponseDto 변환 (좋아요 여부 미포함)
     *
     * @param entity Guide Entity
     * @return GuideResponseDto
     */
    public static GuideResponseDto toResponseDto(Guide entity) {
        return toResponseDto(entity, false);
    }

    /**
     * Guide Entity 리스트 → GuideResponseDto 리스트 변환
     *
     * @param entities Guide Entity 리스트
     * @return GuideResponseDto 리스트
     */
    public static List<GuideResponseDto> toResponseDtoList(List<Guide> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
                .map(GuideMapper::toResponseDto)
                .collect(Collectors.toList());
    }
}
