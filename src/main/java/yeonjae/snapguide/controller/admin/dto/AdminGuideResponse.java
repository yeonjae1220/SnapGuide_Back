package yeonjae.snapguide.controller.admin.dto;

import lombok.Builder;
import lombok.Getter;
import yeonjae.snapguide.domain.guide.Guide;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminGuideResponse {
    private Long id;
    private String tip;
    private String authorNickname;
    private String locationName;
    private int likeCount;
    private int mediaCount;
    private LocalDateTime createdAt;

    public static AdminGuideResponse of(Guide guide) {
        return AdminGuideResponse.builder()
                .id(guide.getId())
                .tip(guide.getTip())
                .authorNickname(guide.getAuthor() != null ? guide.getAuthor().getNickname() : null)
                .locationName(guide.getLocation() != null ? guide.getLocation().getLocationName() : null)
                .likeCount(guide.getLikeCount())
                .mediaCount(guide.getMediaList() != null ? guide.getMediaList().size() : 0)
                .createdAt(guide.getCreatedAt())
                .build();
    }
}
