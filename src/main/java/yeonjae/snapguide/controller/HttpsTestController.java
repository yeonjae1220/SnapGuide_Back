package yeonjae.snapguide.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HttpsTestController {
    @GetMapping("/test-headers")
    public String testHeaders(HttpServletRequest request) {
        String proto = request.getHeader("X-Forwarded-Proto");
        String host = request.getHeader("Host");
        return "X-Forwarded-Proto: " + proto + ", Host: " + host;
    }
}
