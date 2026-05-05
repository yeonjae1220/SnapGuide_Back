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
import java.util.concurrent.ConcurrentHashMap;

@Primary
@Service
@Transactional
@RequiredArgsConstructor
public class LocationServiceGeoImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final ReverseGeocodingService reverseGeocodingService;
    private final LocationSavingService locationSavingService;

    /**
     * 동일 좌표 중복 API 호출 방지를 위한 in-memory 캐시 (애플리케이션 생명주기 동안 유지).
     * DB에 이미 저장된 좌표라면 애플리케이션 재시작 후 첫 조회 시 DB에서 로딩되어 캐시에 적재된다.
     */
    private final ConcurrentHashMap<String, Location> inMemoryCache = new ConcurrentHashMap<>();

    /**
     * 좌표별 lock 객체. 동일 좌표에 대한 Google API 중복 호출을 막는다.
     * synchronized 블록 내에서 LocationSavingService(REQUIRES_NEW)를 호출하므로
     * lock 해제 전 커밋이 완료되어 다른 스레드에서 DB 조회 가능.
     */
    private final ConcurrentHashMap<String, Object> coordinateLocks = new ConcurrentHashMap<>();

    /**
     * 소수점 4자리 반올림 키 (약 11m 정밀도).
     * 같은 장소에서 찍은 사진들이 동일 키로 매핑되어 중복 API 호출을 방지한다.
     */
    private String coordinateKey(double lat, double lng) {
        return Math.round(lat * 10000) + "," + Math.round(lng * 10000);
    }

    @Override
    public Optional<Location> extractAndResolveLocation(byte[] imageBytes) {
        Optional<double[]> coordinate = ExifCoordinateExtractor.extractCoordinate(new ByteArrayInputStream(imageBytes));
        if (coordinate.isEmpty()) {
            return Optional.empty();
        }
        double[] latLng = coordinate.get();
        return Optional.of(resolveWithDedup(latLng[0], latLng[1]));
    }

    @Override
    public Location saveLocation(Double lat, Double lng) {
        return resolveWithDedup(lat, lng);
    }

    /**
     * 중복 제거 위치 조회/저장.
     * 1차) in-memory 캐시 (lock 없이 빠른 경로)
     * 2차) per-coordinate lock → double-check → DB 조회 → 없으면 LocationSavingService(REQUIRES_NEW) 위임
     */
    private Location resolveWithDedup(double lat, double lng) {
        String key = coordinateKey(lat, lng);

        // 1차: in-memory 캐시 빠른 경로
        Location cached = inMemoryCache.get(key);
        if (cached != null) {
            return cached;
        }

        // 2차: per-coordinate lock 후 double-check
        Object lock = coordinateLocks.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            // lock 대기 중 다른 스레드가 처리 완료했을 수 있음
            cached = inMemoryCache.get(key);
            if (cached != null) {
                return cached;
            }

            // DB 조회 (다른 스레드의 REQUIRES_NEW 커밋이 완료되어 있으면 여기서 발견됨)
            List<Location> existing = locationRepository.findLocationByCoordinateNative(lat, lng);
            if (!existing.isEmpty()) {
                Location resolved = resolveOrRefresh(existing.get(0), lat, lng);
                inMemoryCache.put(key, resolved);
                return resolved;
            }

            // Google API 호출 — LocationSavingService(REQUIRES_NEW)가 즉시 커밋
            // → synchronized 블록 내에서 커밋 완료 → lock 해제 전 다른 스레드에 DB 가시적
            Location saved = locationSavingService.saveNewLocation(lat, lng);
            inMemoryCache.put(key, saved);
            return saved;
        }
    }

    // 좌표만 있는 캐시 레코드이면 재역지오코딩하여 in-place 업데이트
    private Location resolveOrRefresh(Location cached, double lat, double lng) {
        if (hasAddressData(cached)) return cached;

        Location fresh = reverseGeocodingService.reverseGeocode(lat, lng).block();
        if (fresh == null) return cached;

        Location updated = cached.toBuilder()
                .locationName(fresh.getLocationName())
                .formattedAddress(fresh.getFormattedAddress())
                .country(fresh.getCountry())
                .countryCode(fresh.getCountryCode())
                .region(fresh.getRegion())
                .city(fresh.getCity())
                .subRegion(fresh.getSubRegion())
                .district(fresh.getDistrict())
                .street(fresh.getStreet())
                .streetNumber(fresh.getStreetNumber())
                .buildingName(fresh.getBuildingName())
                .subPremise(fresh.getSubPremise())
                .postalCode(fresh.getPostalCode())
                .rawJson(fresh.getRawJson())
                .provider(fresh.getProvider())
                .build();
        return locationRepository.save(updated);
    }

    private boolean hasAddressData(Location loc) {
        return loc.getCity() != null || loc.getSubRegion() != null || loc.getFormattedAddress() != null;
    }
}
