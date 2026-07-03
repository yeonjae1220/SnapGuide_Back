package yeonjae.snapguide.service.locationSerivce;

import com.drew.metadata.Metadata;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.domain.location.GeometryUtils;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.domain.media.mediaUtil.exifExtrator.ExifCoordinateExtractor;
import yeonjae.snapguide.repository.locationRepository.LocationRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class LocationServiceCoordImpl implements LocationService{
    private final LocationRepository locationRepository;

    /**
     * 좌표값만 저장 (나중에 api 무료 사용량 다 찼을 때 좌표만 저장하기 위해)
     */

    @Override
    public Optional<Location> extractAndResolveLocation(Metadata metadata) {
        Optional<double[]> coordinate = ExifCoordinateExtractor.extractCoordinate(metadata);
        if (coordinate.isEmpty()) {
            return Optional.empty();  // EXIF 좌표 없음 → 정상 케이스
        }
        double[] latLng = coordinate.get();

        List<Location> existing = locationRepository.findLocationByCoordinateNative(latLng[0], latLng[1]);
        if (!existing.isEmpty()) {
            return Optional.of(existing.get(0));
        }

        Location location = Location.builder()
                .coordinate(GeometryUtils.createPoint(latLng[0], latLng[1]))
                .build();

        return Optional.of(locationRepository.save(location));
    }

    public Location saveLocation(Double lat, Double lng) {
        // Location이 존재할경우 처리

        List<Location> locationByCoordinate = locationRepository.findLocationByCoordinateNative(lat, lng);

        if (!locationByCoordinate.isEmpty()) {
            return locationByCoordinate.get(0); // NOTE : 일단 첫번째 데이터를 반환하는 걸로 해뒀는데,, 일단 어색하다.
        }

        Location location = Location.builder()
//                .latitude(lat)
//                .longitude(lng)
                .coordinate(GeometryUtils.createPoint(lat, lng))
                .build();
        return locationRepository.save(location);
    }
}
