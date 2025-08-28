package yeonjae.snapguide.controller.locationController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yeonjae.snapguide.controller.locationController.locationDto.LocationRequestDto;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.service.locationSerivce.LocationServiceGeoImpl;
import yeonjae.snapguide.service.util.googleMapApiDto.PlaceAutocompleteService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/location")
@Slf4j
public class LocationController {
    private final LocationServiceGeoImpl locationServiceGeoImpl;
    private final PlaceAutocompleteService placeAutocompleteService;

    @PostMapping("/api/upload")
    public ResponseEntity<String> saveLocation(@RequestBody LocationRequestDto requestDto) {
        Location location = locationServiceGeoImpl.saveLocation(requestDto.getLatitude(), requestDto.getLongitude());

        return ResponseEntity.ok("위치 저장 완료");
    }

    @GetMapping("/api/places/autocomplete")
    public ResponseEntity<String> getPlaceAutocomplete(@RequestParam String input) {
        try {
            String results = placeAutocompleteService.getPlaceAutocomplete(input);
            // 🔹 정상 응답일 경우 그대로 반환
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            // 🔹 서비스 로직에서 예외 발생 시 (Google API 호출 실패 등)
            log.error("Place Autocomplete 프록시 중 에러 발생", e);
            // 🔹 클라이언트가 다운되지 않도록 안전한 빈 JSON 응답을 반환
            String safeEmptyResponse = "{\"predictions\": [], \"status\": \"REQUEST_DENIED\"}";
            return ResponseEntity.ok(safeEmptyResponse);
        }
    }

    @GetMapping("/api/places/details")
    public ResponseEntity<String> getPlaceDetails(@RequestParam String placeId) {
        try {
            String result = placeAutocompleteService.getPlaceDetails(placeId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Place Details 프록시 중 에러 발생", e);
            String safeEmptyResponse = "{\"result\": {}, \"status\": \"REQUEST_DENIED\"}";
            return ResponseEntity.ok(safeEmptyResponse);
        }
    }

}
