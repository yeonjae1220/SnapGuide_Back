package yeonjae.snapguide.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.domain.push.PushSubscription;
import yeonjae.snapguide.repository.PushSubscriptionRepository;

import java.security.Security;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final ObjectMapper objectMapper;

    @Value("${pwa.vapid.public-key}")
    private String vapidPublicKey;

    @Value("${pwa.vapid.private-key}")
    private String vapidPrivateKey;

    @Value("${pwa.vapid.subject}")
    private String vapidSubject;

    private nl.martijndwars.webpush.PushService webPushService;

    @PostConstruct
    public void init() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        if (vapidPublicKey == null || vapidPublicKey.isBlank() ||
                vapidPrivateKey == null || vapidPrivateKey.isBlank()) {
            return; // VAPID 키 미설정 시 push 기능 비활성화 (앱 시작은 정상)
        }
        try {
            webPushService = new nl.martijndwars.webpush.PushService(vapidPublicKey, vapidPrivateKey);
            webPushService.setSubject(vapidSubject);
        } catch (Exception e) {
            throw new IllegalStateException("VAPID 키 초기화 실패: " + e.getMessage(), e);
        }
    }

    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

    @Transactional
    public void subscribe(Member member, String endpoint, String auth, String p256dh) {
        Optional<PushSubscription> existing = pushSubscriptionRepository.findByEndpoint(endpoint);
        if (existing.isPresent()) {
            return;
        }
        PushSubscription subscription = PushSubscription.builder()
                .member(member)
                .endpoint(endpoint)
                .auth(auth)
                .p256dh(p256dh)
                .build();
        pushSubscriptionRepository.save(subscription);
    }

    @Transactional
    public void unsubscribe(String endpoint) {
        pushSubscriptionRepository.findByEndpoint(endpoint)
                .ifPresent(sub -> {
                    sub.deactivate();
                    pushSubscriptionRepository.save(sub);
                });
    }

    public void sendNotification(PushSubscription pushSubscription, String title, String body, String url) {
        if (webPushService == null) return;
        try {
            Subscription subscription = new Subscription(
                    pushSubscription.getEndpoint(),
                    new Subscription.Keys(pushSubscription.getP256dh(), pushSubscription.getAuth())
            );

            String payload = objectMapper.writeValueAsString(Map.of(
                    "title", title,
                    "body", body,
                    "url", url != null ? url : "/"
            ));

            Notification notification = new Notification(subscription, payload);
            webPushService.send(notification);
        } catch (Exception e) {
            log.error("푸시 알림 전송 실패: endpoint={}", pushSubscription.getEndpoint(), e);
        }
    }

    public void sendToMember(Long memberId, String title, String body, String url) {
        List<PushSubscription> subscriptions = pushSubscriptionRepository.findAllByMemberIdAndActiveTrue(memberId);
        for (PushSubscription sub : subscriptions) {
            sendNotification(sub, title, body, url);
        }
    }

    public void broadcast(String title, String body, String url) {
        List<PushSubscription> subscriptions = pushSubscriptionRepository.findAllByActiveTrue();
        for (PushSubscription sub : subscriptions) {
            sendNotification(sub, title, body, url);
        }
    }
}
