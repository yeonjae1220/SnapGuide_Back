package yeonjae.snapguide.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import yeonjae.snapguide.controller.admin.dto.AdminMemberResponse;
import yeonjae.snapguide.controller.admin.dto.AdminRoleUpdateRequest;
import yeonjae.snapguide.controller.admin.dto.AdminStatsResponse;
import yeonjae.snapguide.service.admin.AdminService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    /**
     * 전체 회원 목록 조회 (페이지네이션)
     */
    @GetMapping("/members")
    public ResponseEntity<Page<AdminMemberResponse>> getMembers(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminService.getMembers(pageable));
    }

    /**
     * 회원 역할 변경 (MEMBER ↔ ADMIN)
     */
    @PatchMapping("/members/{id}/role")
    public ResponseEntity<AdminMemberResponse> updateMemberRole(
            @PathVariable Long id,
            @Valid @RequestBody AdminRoleUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateMemberRole(id, request));
    }

    /**
     * 전체 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }
}
