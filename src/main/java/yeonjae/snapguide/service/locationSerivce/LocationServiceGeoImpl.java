package yeonjae.snapguide.service.locationSerivce;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.domain.media.mediaUtil.exifExtrator.ExifCoordinateExtractor;
import yeonjae.snapguide.repository.locationRepository.LocationRepository;
import yeonjae.snapguide.service.ReverseGeocodingService;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

@Primary  // LocationService 빈 충돌 시 기본(Geo 버전) 선택
@Service
@Transactional
@RequiredArgsConstructor
public class LocationServiceGeoImpl implements LocationService {
    private final LocationRepository locationRepository;
    private final ReverseGeocodingService reverseGeocodingService;

    @Override
    public Optional<Location> extractAndResolveLocation(byte[] imageBytes) {
        Optional<double[]> coordinate = ExifCoordinateExtractor.extractCoordinate(new ByteArrayInputStream(imageBytes));
        if (coordinate.isEmpty()) {
            return Optional.empty();  // EXIF 좌표 없음 → 정상 케이스
        }
        double[] latLng = coordinate.get();

        List<Location> existing = locationRepository.findLocationByCoordinateNative(latLng[0], latLng[1]);
        if (!existing.isEmpty()) {
            return Optional.of(existing.get(0));
        }

        Location location = reverseGeocodingService.reverseGeocode(latLng[0], latLng[1]).block();
        if (location == null) {
            throw new IllegalStateException("Reverse geocoding failed for lat=" + latLng[0] + ", lng=" + latLng[1]);
        }
        return Optional.of(locationRepository.save(location));
    }

    // 사용자가 지정한 좌표 값을 받아 location 저장, google map api
    public Location saveLocation(Double lat, Double lng) {
        // Location이 존재할경우 처리

        List<Location> locationByCoordinate = locationRepository.findLocationByCoordinateNative(lat, lng);

        if (!locationByCoordinate.isEmpty()) {
            return locationByCoordinate.get(0); // NOTE : 일단 첫번째 데이터를 반환하는 걸로 해뒀는데,, 일단 어색하다.
        }

        Location location = reverseGeocodingService.reverseGeocode(lat, lng).block();
        if (location == null) {
            throw new IllegalStateException("Reverse geocoding failed for lat=" + lat + ", lng=" + lng);
        }
        return locationRepository.save(location);
    }

}
/**
 * TODO
 * 🔎 block()의 위험성 간단 정리
 * 	•	block()은 Reactive 흐름을 막고 동기식으로 대기합니다.
 * 	•	테스트나 초기 개발 단계에서는 괜찮지만, 웹 요청 처리 쓰레드에서 사용할 경우 성능 저하 및 deadlock 위험이 있습니다.
 * 	•	서비스 계층에서는 가능하면 비동기로 .subscribe()나 .flatMap() 등을 사용하는 것이 더 안전합니다.
 *
 * 단, 지금처럼 단발성 위치 조회를 동기 흐름에서 처리하는 것은 제한적으로 block() 사용이 허용됩니다. 하지만 나중에 병렬 업로드나 Reactive 체계를 도입한다면 반드시 제거해야 합니다.
 */

/**
 *     @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST) // PERSIST: 새 Location일 경우 자동 저장
 *     @JoinColumn(name = "location_id")
 *     private Location location;
 *     Media Entity에서 위 코드를 통해 저장됨
 */