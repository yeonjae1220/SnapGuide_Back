package yeonjae.snapguide.domain.guide;

import lombok.AllArgsConstructor;
import lombok.Data;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.domain.media.Media;
import yeonjae.snapguide.domain.media.MediaDto;
import yeonjae.snapguide.domain.member.Member;

import java.util.List;

@Data
@AllArgsConstructor
public class GuideDto {
    private Long id;
    private String tip;
    private String author; // 이런것도 다 dto로 만들어 둔걸로 보내기
    private String locationName;
    private List<MediaDto> media;

    public static GuideDto fromEntity(Guide guide) {
        List<MediaDto> mediaDto = guide.getMediaList()
                .stream()
                .map(MediaDto::fromEntity)
                .toList();

        return new GuideDto(
                guide.getId(),
                guide.getTip(),
                guide.getAuthor().getNickname(),
                guide.getLocation().getLocationName(),
                mediaDto
        );
    }

}
