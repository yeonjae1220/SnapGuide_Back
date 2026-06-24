package yeonjae.snapguide.controller.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yeonjae.snapguide.controller.admin.dto.AdminStatsResponse;
import yeonjae.snapguide.service.admin.AdminService;

import java.util.List;
import java.util.Map;

/**
 * Read-only summary consumed by the lab.mungji console aggregator. Returns
 * aggregate counts only (no PII), gated by {@code ServiceTokenAuthFilter} on the
 * {@code /api/internal/**} security chain.
 */
@RestController
@RequestMapping("/api/internal/console")
@RequiredArgsConstructor
public class InternalConsoleController {

    private final AdminService adminService;

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        AdminStatsResponse stats = adminService.getStats();
        return Map.of(
                "totalUsers", stats.getTotalMembers(),
                "usage", List.of(
                        Map.of("label", "가이드", "value", stats.getTotalGuides()),
                        Map.of("label", "댓글", "value", stats.getTotalComments()),
                        Map.of("label", "장소", "value", stats.getTotalLocations())
                ));
    }
}
