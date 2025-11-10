package yeonjae.snapguide.controller.guideController.guideDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import yeonjae.snapguide.domain.guide.Guide;
import yeonjae.snapguide.domain.guide.GuideMapper;
import yeonjae.snapguide.domain.media.MediaDto;
import yeonjae.snapguide.domain.member.dto.MemberDto;

import java.util.List;

/**
 * Guide Entity 응답 DTO
 * 클라이언트에게 전달할 Guide 정보를 담음
 */
@Data
@AllArgsConstructor
@Builder
public class GuideResponseDto {
    private Long id;
    private String tip;
    private MemberDto author;
    private String locationName;   // ex) "서울 성동구…"
    private List<MediaDto> media;
    private int likeCount;
    private boolean userHasLiked;

    /**
     * @deprecated GuideMapper.toResponseDto()를 사용하세요
     * 하위 호환성을 위해 유지되지만 내부적으로 GuideMapper 위임
     */
    @Deprecated
    public static GuideResponseDto of(Guide guide, boolean userHasLiked) {
        return GuideMapper.toResponseDto(guide, userHasLiked);
    }
}
