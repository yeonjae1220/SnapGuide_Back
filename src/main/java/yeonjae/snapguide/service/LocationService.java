package yeonjae.snapguide.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.entity.guide.Location;
import yeonjae.snapguide.entity.guide.mediaUtil.exifUtil.extrator.ExifCoordinateExtractor;

import java.io.File;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
class LocationService {
    private final ReverseGeocodingService reverseGeocodingService;
    // 좌표 값 추출 && 저장
    public Location extractAndResolveLocation(File file) {
        Optional<double[]> coordinate = ExifCoordinateExtractor.extractCoordinate(file);
        // TODO : exception 날릴께 아니고 그냥 Null이나 default값 저장하는 걸로 바꾸기. 좌표값 없는 사진 유형도 많을 듯 함
        double[] latLng = coordinate.orElseThrow(() ->
                new IllegalArgumentException("좌표 정보가 없습니다."));
        Location location = reverseGeocodingService.reverseGeocode(latLng[0], latLng[1]).block();
        // 2. 해당 Guide 찾기 TODO : Media, Location과 Guide 연관관계 연결 해줘야함
        if (location == null) {
            throw new IllegalStateException("Reverse geocoding failed for lat=" + latLng[0] + ", lng=" + latLng[1]);
        }
        return location;
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