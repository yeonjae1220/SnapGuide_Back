package yeonjae.snapguide.controller.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminStatsResponse {
    private long totalMembers;
    private long totalGuides;
}
