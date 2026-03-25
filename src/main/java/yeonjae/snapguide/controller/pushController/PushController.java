package yeonjae.snapguide.controller.pushController;

import jakarta.validation.Valid;
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
    public ResponseEntity<Void> subscribe(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid PushSubscribeRequest request) {
        Member member = getMember(userDetails);
        pushService.subscribe(member, request.endpoint(), request.keys().auth(), request.keys().p256dh());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {
        pushService.unsubscribe(body.get("endpoint"));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test")
    public ResponseEntity<Void> testNotification(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) Map<String, String> body) {
        Member member = getMember(userDetails);
        String title = body != null ? body.getOrDefault("title", "SnapGuide 테스트 알림") : "SnapGuide 테스트 알림";
        String message = body != null ? body.getOrDefault("body", "푸시 알림이 정상 동작합니다!") : "푸시 알림이 정상 동작합니다!";
        String url = body != null ? body.getOrDefault("url", "/") : "/";
        pushService.sendToMember(member.getId(), title, message, url);
        return ResponseEntity.noContent().build();
    }

    private Member getMember(UserDetails userDetails) {
        return memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("no member: " + userDetails.getUsername()));
    }
}
