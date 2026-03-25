package yeonjae.snapguide.controller.guideController.guideDto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuideUpdateRequestDto {
    @NotBlank(message = "tip은 필수입니다.")
    private String tip;
}
