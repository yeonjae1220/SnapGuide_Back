package yeonjae.snapguide.service.locationSerivce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.domain.location.GeometryUtils;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.domain.media.mediaUtil.exifExtrator.ExifCoordinateExtractor;
import yeonjae.snapguide.repository.locationRepository.LocationRepository;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
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

    public Location extractAndResolveLocation(byte[] imageBytes) {
        Optional<double[]> coordinate = ExifCoordinateExtractor.extractCoordinate(new ByteArrayInputStream(imageBytes));
        if (coordinate.isEmpty()) {
            return null;
        }
        double[] latLng = coordinate.orElseThrow(() ->
                new IllegalArgumentException("좌표 정보가 없습니다."));

        // Location이 존재할경우 처리

        List<Location> locationByCoordinate = locationRepository.findLocationByCoordinateNative(latLng[0], latLng[1]);

        if (!locationByCoordinate.isEmpty()) {
            return locationByCoordinate.get(0); // NOTE : 일단 첫번째 데이터를 반환하는 걸로 해뒀는데,, 일단 어색하다.
        }

        Location location = Location.builder()
//                .latitude(latLng[0])
//                .longitude(latLng[1])
                .coordinate(GeometryUtils.createPoint(latLng[0], latLng[1]))
                .build();

        return locationRepository.save(location);
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
