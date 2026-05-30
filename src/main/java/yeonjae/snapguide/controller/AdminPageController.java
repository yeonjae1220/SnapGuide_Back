package yeonjae.snapguide.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import yeonjae.snapguide.domain.member.Authority;
import yeonjae.snapguide.service.admin.AdminService;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPageController {

    private final AdminService adminService;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("page", "dashboard");
        model.addAttribute("stats", adminService.getStats());
        return "admin/dashboard";
    }

    @GetMapping("/members")
    public String members(@RequestParam(defaultValue = "0") int p, Model model) {
        var pageResult = adminService.getMembers(
                PageRequest.of(p, 20, Sort.by(Sort.Direction.DESC, "id")));
        model.addAttribute("page", "members");
        model.addAttribute("members", pageResult.getContent());
        model.addAttribute("currentPage", p);
        model.addAttribute("totalPages", pageResult.getTotalPages());
        model.addAttribute("totalElements", pageResult.getTotalElements());
        return "admin/members";
    }

    @PostMapping("/members/{id}/role")
    public String updateRole(@PathVariable Long id,
                             @RequestParam String authority,
                             RedirectAttributes ra) {
        adminService.toggleMemberAuthority(id, Authority.valueOf(authority));
        ra.addFlashAttribute("successMessage", "역할이 변경되었습니다.");
        return "redirect:/admin/members";
    }

    @GetMapping("/guides")
    public String guides(@RequestParam(defaultValue = "0") int p, Model model) {
        var pageResult = adminService.getGuides(
                PageRequest.of(p, 20, Sort.by(Sort.Direction.DESC, "id")));
        model.addAttribute("page", "guides");
        model.addAttribute("guides", pageResult.getContent());
        model.addAttribute("currentPage", p);
        model.addAttribute("totalPages", pageResult.getTotalPages());
        model.addAttribute("totalElements", pageResult.getTotalElements());
        return "admin/guides";
    }

    @PostMapping("/guides/{id}/delete")
    public String deleteGuide(@PathVariable Long id, RedirectAttributes ra) {
        adminService.deleteGuide(id);
        ra.addFlashAttribute("successMessage", "가이드가 삭제되었습니다.");
        return "redirect:/admin/guides";
    }

    @GetMapping("/comments")
    public String comments(@RequestParam(defaultValue = "0") int p, Model model) {
        var pageResult = adminService.getComments(
                PageRequest.of(p, 20, Sort.by(Sort.Direction.DESC, "id")));
        model.addAttribute("page", "comments");
        model.addAttribute("comments", pageResult.getContent());
        model.addAttribute("currentPage", p);
        model.addAttribute("totalPages", pageResult.getTotalPages());
        model.addAttribute("totalElements", pageResult.getTotalElements());
        return "admin/comments";
    }

    @PostMapping("/comments/{id}/delete")
    public String deleteComment(@PathVariable Long id, RedirectAttributes ra) {
        adminService.deleteComment(id);
        ra.addFlashAttribute("successMessage", "댓글이 삭제되었습니다.");
        return "redirect:/admin/comments";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }
}
