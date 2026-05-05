package yeonjae.snapguide.service.locationSerivce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.repository.locationRepository.LocationRepository;
import yeonjae.snapguide.service.ReverseGeocodingService;

import java.util.List;

/**
 * 위치 정보 저장 전용 서비스.
 *
 * REQUIRES_NEW 전파로 호출 즉시 별도 트랜잭션을 열고 커밋한다.
 * LocationServiceGeoImpl 의 synchronized 블록 안에서 호출되어,
 * lock 해제 전에 커밋이 완료됨으로써 다른 스레드가 DB에서 즉시 확인 가능하다.
 * (synchronized + @Transactional Race Condition 문제 해결)
 */
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
@RequiredArgsConstructor
@Slf4j
public class LocationSavingService {

    private final LocationRepository locationRepository;
    private final ReverseGeocodingService reverseGeocodingService;

    /**
     * 좌표에 해당하는 위치를 저장한다.
     * 다른 스레드가 먼저 저장했을 수 있으므로 DB를 한 번 더 확인 후 저장(double-check).
     *
     * @param lat 위도
     * @param lng 경도
     * @return 저장된 (또는 이미 존재하는) Location 엔티티
     */
    public Location saveNewLocation(double lat, double lng) {
        // double-check: synchronized 블록 진입 후 다른 스레드가 먼저 커밋했을 수 있음
        List<Location> existing = locationRepository.findLocationByCoordinateNative(lat, lng);
        if (!existing.isEmpty()) {
            log.debug("[LocationSavingService] Location already exists for ({}, {}), skipping API call", lat, lng);
            return existing.get(0);
        }

        log.info("[LocationSavingService] Calling Google Reverse Geocoding API for ({}, {})", lat, lng);
        Location location = reverseGeocodingService.reverseGeocode(lat, lng).block();
        if (location == null) {
            throw new IllegalStateException(
                    "Reverse geocoding returned null for lat=" + lat + ", lng=" + lng);
        }

        Location saved = locationRepository.save(location);
        log.info("[LocationSavingService] Saved new Location id={} for ({}, {})", saved.getId(), lat, lng);
        return saved;
        // 메서드 리턴 시 REQUIRES_NEW 트랜잭션이 즉시 커밋 → 다른 스레드에서 DB 조회 가능
    }
}
