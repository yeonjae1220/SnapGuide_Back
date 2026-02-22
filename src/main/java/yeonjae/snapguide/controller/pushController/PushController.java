package yeonjae.snapguide.controller.pushController;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;
import yeonjae.snapguide.service.PushService;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/push")
public class PushController {

    private final PushService pushService;
    private final MemberRepository memberRepository;

    @GetMapping("/vapid-public-key")
    public ResponseEntity<String> getVapidPublicKey() {
        return ResponseEntity.ok(pushService.getVapidPublicKey());
    }

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> body) {

        Member member = getMember(userDetails);

        String endpoint = (String) body.get("endpoint");
        @SuppressWarnings("unchecked")
        Map<String, String> keys = (Map<String, String>) body.get("keys");
        String auth = keys.get("auth");
        String p256dh = keys.get("p256dh");

        pushService.subscribe(member, endpoint, auth, p256dh);
        return ResponseEntity.ok(Map.of("message", "구독 완료"));
    }

    @DeleteMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribe(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {

        String endpoint = body.get("endpoint");
        pushService.unsubscribe(endpoint);
        return ResponseEntity.ok(Map.of("message", "구독 해제 완료"));
    }

    @PostMapping("/test")
    public ResponseEntity<?> testNotification(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) Map<String, String> body) {

        Member member = getMember(userDetails);
        String title = body != null ? body.getOrDefault("title", "SnapGuide 테스트 알림") : "SnapGuide 테스트 알림";
        String message = body != null ? body.getOrDefault("body", "푸시 알림이 정상 동작합니다!") : "푸시 알림이 정상 동작합니다!";
        String url = body != null ? body.getOrDefault("url", "/") : "/";

        pushService.sendToMember(member.getId(), title, message, url);
        return ResponseEntity.ok(Map.of("message", "테스트 알림 전송 완료"));
    }

    private Member getMember(UserDetails userDetails) {
        String email = userDetails.getUsername();
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("no member: " + email));
    }
}
