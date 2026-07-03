package yeonjae.snapguide.service.locationSerivce;

import com.drew.metadata.Metadata;
import yeonjae.snapguide.domain.location.Location;

import java.util.Optional;

public interface LocationService {
    /**
     * 이미지 EXIF(이미 파싱된 Metadata)에서 좌표를 추출하여 Location으로 해석.
     * EXIF 좌표가 없으면 Optional.empty() 반환.
     */
    Optional<Location> extractAndResolveLocation(Metadata metadata);

    Location saveLocation(Double lat, Double lng);
}
