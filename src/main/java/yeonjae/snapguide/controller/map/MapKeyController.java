package yeonjae.snapguide.controller.map;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yeonjae.snapguide.service.map.MapService;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/maps")
public class MapKeyController {
    private final MapService mapService;

    @GetMapping("/key")
    public ResponseEntity<Map<String, String>> getMapApiKey() {
        Map<String, String> apiKeyMap = mapService.getGoogleMapsApiKey();
        return ResponseEntity.ok(apiKeyMap);
    }
}
