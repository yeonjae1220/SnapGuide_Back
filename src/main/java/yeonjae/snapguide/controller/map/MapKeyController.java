package yeonjae.snapguide.controller.map;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yeonjae.snapguide.security.ClientIpResolver;
import yeonjae.snapguide.security.ratelimit.RateLimiterService;
import yeonjae.snapguide.service.map.LocationResponse;
import yeonjae.snapguide.service.map.MapService;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/maps")
public class MapKeyController {
    private final MapService mapService;
    private final RateLimiterService rateLimiterService;
    private final ClientIpResolver clientIpResolver;

    @GetMapping("/key")
    public ResponseEntity<Map<String, String>> getMapApiKey(HttpServletRequest request) {
        rateLimiterService.checkMapKeyRate(clientIpResolver.resolve(request));
        return ResponseEntity.ok(mapService.getGoogleMapsApiKey());
    }

    @GetMapping("/location")
    public ResponseEntity<LocationResponse> getIpLocation(HttpServletRequest request) {
        rateLimiterService.checkLocationRate(clientIpResolver.resolve(request));
        return mapService.getIpLocation(request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
