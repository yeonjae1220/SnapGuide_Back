package yeonjae.snapguide.service.locationSerivce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.domain.media.mediaUtil.exifExtrator.ExifCoordinateExtractor;
import yeonjae.snapguide.repository.locationRepository.LocationRepository;

import java.io.File;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class LocationServiceCoordImpl implements LocationService{
    private final LocationRepository locationRepository;

    /**
     * 좌표값만 저장 (나중에 api 무료 사용량 다 찼을 때 좌표만 저장하기 위해)
     */

    public Location extractAndResolveLocation(File file) {
        Optional<double[]> coordinate = ExifCoordinateExtractor.extractCoordinate(file);
        if (coordinate.isEmpty()) {
            return null;
        }
        double[] latLng = coordinate.orElseThrow(() ->
                new IllegalArgumentException("좌표 정보가 없습니다."));

        Location location = Location.builder()
                .latitude(latLng[0])
                .longitude(latLng[1])
                .build();

        return locationRepository.save(location);
    }

    public Location saveLocation(Double lat, Double lng) {
        Location location = Location.builder()
                .latitude(lat)
                .longitude(lng)
                .build();
        return locationRepository.save(location);
    }
}
