package yeonjae.snapguide.controller.guideController.guideDto;

import lombok.AllArgsConstructor;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import yeonjae.snapguide.domain.guide.Guide;
import yeonjae.snapguide.domain.media.MediaDto;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.domain.member.dto.MemberDto;

import java.util.List;

@Data
@AllArgsConstructor

@Builder
public class GuideResponseDto {
    private Long id;
    private String tip;
    private MemberDto author;
    private String locationName;   // ex) “서울 성동구…” // location으로 보내는게 낫나?
    private List<MediaDto> media;
    private int likeCount;
    private boolean userHasLiked;



    // 필요하다면 이런 형태의 새로운 static 메서드를 만들 수 있습니다. (솔루션 2 참고)
    public static GuideResponseDto of(Guide guide, boolean userHasLiked) {
        List<MediaDto> mediaDto = guide.getMediaList()
                .stream()
                .map(MediaDto::fromEntity)
                .toList();

        return GuideResponseDto.builder()
                .id(guide.getId())
                .tip(guide.getTip())
                .author(MemberDto.fromEntity(guide.getAuthor()))
                .locationName(guide.getLocation().getLocationName())
                .media(mediaDto)
                .likeCount(guide.getLikeCount())
                .userHasLiked(userHasLiked) // 파라미터로 받은 값을 사용
                .build();
    }

}
