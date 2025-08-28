package yeonjae.snapguide.controller.guideController.guideDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import yeonjae.snapguide.domain.media.MediaDto;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class GuideResponseDto {
    private Long id;
    private String tip;
    private Long authorId;
    private String locationName;   // ex) “서울 성동구…” // location으로 보내는게 낫나?
    private List<MediaDto> media;
}
