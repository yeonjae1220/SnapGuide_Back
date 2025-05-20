package yeonjae.snapguide.controller.guideController.guideDto;

import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class GuideCreateTestDto {
    private Long memberId;
    private String tip;
    @Nullable // location을 선택하지 않은 경우 null로 올 수 있음
    private Long locationId;
    private List<Long> mediaIds; // 선택한 미디어 ID 리스트

    private GuideCreateTestDto(Long memberId, String tip, Long locationId) {
        this.memberId = memberId;
        this.tip = tip;
        this.locationId = locationId;
        this.mediaIds = new ArrayList<>();
    }

    public static GuideCreateTestDto of(Long memberId, String tip, Long locationId) {
        return new GuideCreateTestDto(memberId, tip, locationId);
    }


}
// @Nullable은 lombok이나 validation의 것이 아닌 JavaDoc 용 주석이기도 하며, 생략해도 Spring에서 문제는 되지 않음