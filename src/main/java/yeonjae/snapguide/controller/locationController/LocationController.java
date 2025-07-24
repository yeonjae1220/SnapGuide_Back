package yeonjae.snapguide.controller.locationController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yeonjae.snapguide.controller.locationController.locationDto.LocationRequestDto;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.service.locationSerivce.LocationService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/location")
@Slf4j
public class LocationController {
    private final LocationService locationService;

    @PostMapping("/api/upload")
    public ResponseEntity<String> saveLocation(@RequestBody LocationRequestDto requestDto) {
        Location location = locationService.saveGeoLocation(requestDto.getLatitude(), requestDto.getLongitude());

        return ResponseEntity.ok("위치 저장 완료");
    }

}
