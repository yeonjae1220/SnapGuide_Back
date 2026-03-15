package yeonjae.snapguide.service.locationSerivce;

import yeonjae.snapguide.domain.location.Location;

import java.util.Optional;

public interface LocationService {
    /**
     * 이미지 EXIF에서 좌표를 추출하여 Location으로 해석.
     * EXIF 좌표가 없으면 Optional.empty() 반환.
     */
    Optional<Location> extractAndResolveLocation(byte[] imageBytes);

    Location saveLocation(Double lat, Double lng);
}
