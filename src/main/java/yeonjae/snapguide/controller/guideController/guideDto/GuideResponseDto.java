package yeonjae.snapguide.controller.guideController.guideDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import yeonjae.snapguide.domain.media.MediaDto;

import java.util.List;

@Data
@AllArgsConstructor
public class GuideResponseDto {
    private Long guideId;
    private String tip;
    private String locationName;   // ex) “서울 성동구…” // location으로 보내는게 낫나?
    private List<MediaDto> media;
}
