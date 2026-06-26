package yeonjae.snapguide.domain.guide;

import org.locationtech.jts.geom.Point;
import yeonjae.snapguide.controller.guideController.guideDto.GuideResponseDto;
import yeonjae.snapguide.domain.location.Location;
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
    private static final String PRIVATE_LOCATION_LABEL = "비공개";

    /**
     * Guide Entity → GuideResponseDto 변환
     * Lazy Loading 방지를 위해 트랜잭션 내에서 호출 필요
     *
     * @param entity Guide Entity
     * @param userHasLiked 사용자 좋아요 여부
     * @return GuideResponseDto
     */
    public static GuideResponseDto toResponseDto(Guide entity, boolean userHasLiked) {
        return toResponseDto(entity, userHasLiked, false);
    }

    public static GuideResponseDto toResponseDtoWithAuthorEmail(Guide entity, boolean userHasLiked) {
        return toResponseDto(entity, userHasLiked, true);
    }

    private static GuideResponseDto toResponseDto(Guide entity, boolean userHasLiked, boolean includeAuthorEmail) {
        if (entity == null) {
            return null;
        }

        // Media 리스트 변환
        List<MediaDto> mediaDtos = entity.getMediaList()
                .stream()
                .map(MediaMapper::toDto)
                .collect(Collectors.toList());

        // 공개 지도 탐색 응답에는 로그인 식별자인 이메일을 노출하지 않는다.
        MemberDto authorDto = includeAuthorEmail
                ? MemberMapper.toDto(entity.getAuthor())
                : MemberMapper.toPublicDto(entity.getAuthor());

        // Location 이름 + 좌표 추출 (null-safe, 구형 레코드 fallback 포함)
        String locationName = null;
        Double latitude = null, longitude = null;
        if (entity.getLocation() != null) {
            locationName = deriveLocationName(entity.getLocation());
            Point coord = entity.getLocation().getCoordinate();
            if (coord != null) {
                latitude  = coord.getY();
                longitude = coord.getX();
            }
        }

        boolean pub = entity.isLocationPublic();
        return GuideResponseDto.builder()
                .id(entity.getId())
                .tip(entity.getTip())
                .author(authorDto)
                .locationName(pub ? locationName : PRIVATE_LOCATION_LABEL)
                .latitude(pub ? latitude : null)
                .longitude(pub ? longitude : null)
                .locationPublic(pub)
                .media(mediaDtos)
                .likeCount(entity.getLikeCount())
                .userHasLiked(userHasLiked)
                .build();
    }

    private static String deriveLocationName(Location loc) {
        if (loc.getLocationName() != null) return loc.getLocationName();
        String sub = loc.getSubRegion(), city = loc.getCity(),
               dis = loc.getDistrict(), reg  = loc.getRegion();
        if (sub  != null && city != null) return sub  + ", " + city;
        if (dis  != null && city != null) return dis  + ", " + city;
        if (city != null && reg  != null) return city + ", " + reg;
        if (reg  != null) return reg;
        return loc.getFormattedAddress();
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
