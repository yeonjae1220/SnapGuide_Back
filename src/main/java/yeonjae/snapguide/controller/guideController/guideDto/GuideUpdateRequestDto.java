package yeonjae.snapguide.controller.guideController.guideDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GuideUpdateRequestDto {
    private Long id;
    private String tip;
}
