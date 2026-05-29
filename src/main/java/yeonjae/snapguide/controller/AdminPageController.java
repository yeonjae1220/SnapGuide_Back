package yeonjae.snapguide.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminPageController {

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("page", "dashboard");
        return "admin/dashboard";
    }

    @GetMapping("/members")
    public String members(Model model) {
        model.addAttribute("page", "members");
        return "admin/dashboard";
    }

    @GetMapping("/guides")
    public String guides(Model model) {
        model.addAttribute("page", "guides");
        return "admin/dashboard";
    }

    @GetMapping("/comments")
    public String comments(Model model) {
        model.addAttribute("page", "comments");
        return "admin/dashboard";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }
}
