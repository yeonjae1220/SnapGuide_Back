package yeonjae.snapguide.service.map;

import io.netty.channel.ChannelOption;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import yeonjae.snapguide.service.config.GoogleMapsConfig;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MapService {

    private final GoogleMapsConfig googleMapsConfig;

    private static final WebClient IPAPI_CLIENT = WebClient.builder()
            .baseUrl("https://ipapi.co")
            .clientConnector(new ReactorClientHttpConnector(
                    HttpClient.create()
                            .responseTimeout(Duration.ofSeconds(3))
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)))
            .build();

    public Map<String, String> getGoogleMapsApiKey() {
        return Map.of("apiKey", googleMapsConfig.getKey());
    }

    public Optional<LocationResponse> getIpLocation(HttpServletRequest request) {
        // forward-headers-strategy: framework 설정으로 Spring이 신뢰 체인의 X-Forwarded-For를
        // 이미 반영했으므로 getRemoteAddr()만 사용한다 (직접 헤더 읽기 금지 — IP 스푸핑 방지)
        String ip = request.getRemoteAddr();
        if (ip == null || ip.isBlank() || isLocalAddress(ip)) {
            return Optional.empty();
        }
        try {
            IpapiResponse body = IPAPI_CLIENT.get()
                    .uri("/{ip}/json/", ip)
                    .retrieve()
                    .bodyToMono(IpapiResponse.class)
                    .timeout(Duration.ofSeconds(3))
                    .block();
            if (body == null || body.latitude() == null || body.longitude() == null) {
                return Optional.empty();
            }
            return Optional.of(new LocationResponse(body.latitude(), body.longitude()));
        } catch (Exception e) {
            log.warn("IP geolocation failed for ip={}: {}", ip, e.getMessage());
            return Optional.empty();
        }
    }

    private boolean isLocalAddress(String ip) {
        return ip.equals("127.0.0.1")
                || ip.equals("::1")
                || ip.equals("0:0:0:0:0:0:0:1")
                || ip.startsWith("10.")
                || ip.startsWith("192.168.")
                || ip.startsWith("169.254.")
                || isRfc1918_172(ip)
                || ip.startsWith("fc00:")
                || ip.startsWith("fd");
    }

    private boolean isRfc1918_172(String ip) {
        if (!ip.startsWith("172.")) return false;
        String[] parts = ip.split("\\.");
        if (parts.length < 2) return false;
        try {
            int second = Integer.parseInt(parts[1]);
            return second >= 16 && second <= 31;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private record IpapiResponse(Double latitude, Double longitude) {}
}
