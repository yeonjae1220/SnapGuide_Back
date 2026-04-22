package yeonjae.snapguide.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPageController {

    @GetMapping("/admin")
    public String adminRedirect() {
        return "redirect:/admin.html";
    }
}
