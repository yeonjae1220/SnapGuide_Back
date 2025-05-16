package yeonjae.snapguide.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.entity.guide.Location;
import yeonjae.snapguide.repository.LocationRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Transactional
class ReverseGeocodingServiceTest {
    @Autowired
    private ReverseGeocodingService reverseGeocodingService;

    @Autowired
    private LocationRepository locationRepository;
    @Test
    public void reverseGeocodeTest() {
        /**
         * WebClient를 사용하는 비동기 코드라 기서 .subscribe(...)는 비동기 콜백으로 실행되기 때문에, System.out.println(...)은 메인 테스트 스레드가 끝나기 전에 아직 호출되지 않았을 수 있습니다. 즉, 테스트가 먼저 종료되어 출력이 안 보이는 것입니다.
         */
//        reverseGeocodingService.reverseGeocode(37.5665, 126.9780)
//                .subscribe(response -> {
//                    System.out.println("응답: " + response);
//                });

        Location response = reverseGeocodingService.reverseGeocode(35.2021804,128.7078053)
                .block(); // 동기적으로 응답을 기다림

        System.out.println("응답: " + response);
        Assertions.assertNotNull(response); // 예시로 응답 검증도 가능

    }

    @Test
    void reverseGeocodeAndSaveLocation() {
        // 예시 좌표: 서울 시청 (37.5665, 126.9780)
        double lat = 37.5665;
        double lng = 126.9780;

        Location location = reverseGeocodingService.reverseGeocode(lat, lng).block(); // block은 테스트용
        System.out.println("location = " + location);

        assertNotNull(location, "Location 객체가 null이 아니어야 합니다.");
        assertNotNull(location.getLocationName(), "주소 문자열이 null이 아니어야 합니다.");
        assertEquals(lat, location.getLatitude());
        assertEquals(lng, location.getLongitude());

        // 저장
        locationRepository.save(location);

        // 저장된 Location이 다시 조회되는지 확인
        Optional<Location> saved = locationRepository.findById(location.getId());
        assertTrue(saved.isPresent(), "저장된 Location이 DB에 존재해야 합니다.");
        assertEquals(location.getLocationName(), saved.get().getLocationName());


    }



}