package yeonjae.snapguide.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import yeonjae.snapguide.controller.locationController.locationDto.LocationRequestDto;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.domain.media.mediaUtil.exifExtrator.ExifCoordinateExtractor;
import yeonjae.snapguide.repository.locationRepository.LocationRepository;
import yeonjae.snapguide.service.locationSerivce.LocationService;

import java.io.File;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {
    @Mock
    private ReverseGeocodingService reverseGeocodingService;
    @InjectMocks
    private LocationService locationService;
    @Mock
    private LocationRepository locationRepository;

    @Test
    void extractAndResolveLocation_shouldReturnLocation() {
        // given
        File dummyFile = new File("src/test/resources/sample.jpg");

        try (MockedStatic<ExifCoordinateExtractor> mockExtractor = mockStatic(ExifCoordinateExtractor.class)) {
            double[] coords = new double[]{37.5665, 126.9780}; // Seoul
            Location mockLocation = Location.builder()
                    .locationName("Gwanggyo Lake Park")
                    .latitude(37.2752)
                    .longitude(127.0469)
                    .country("South Korea")
                    .region("Gyeonggi-do")
                    .subRegion("Suwon-si")
                    .locality("Yeongtong-gu")
                    .route("Gwanggyo-ro")
                    .streetNumber("123")
                    .premise("Lakeside Plaza")
                    .subPremise("Cafe Blossom")
                    .build();

            mockExtractor.when(() -> ExifCoordinateExtractor.extractCoordinate(dummyFile))
                    .thenReturn(Optional.of(coords));

            when(reverseGeocodingService.reverseGeocode(37.5665, 126.9780)).thenReturn(Mono.just(mockLocation));
            when(locationRepository.save(any(Location.class))).thenReturn(mockLocation);
            // when
            Location location = locationService.extractAndResolveLocation(dummyFile);

            // then
            assertNotNull(location);
            assertEquals("Gwanggyo Lake Park", location.getLocationName());
        }
    }

    @Test
    void saveLocation_work_well() {
        // given
        double lat = 37.5665;
        double lng = 126.9780;

        Location mockLocation = Location.builder()
                .latitude(lat)
                .longitude(lng)
                .build();

        // mock 설정
        Mockito.when(reverseGeocodingService.reverseGeocode(lat, lng)).thenReturn(Mono.just(mockLocation));
        Mockito.when(locationRepository.save(mockLocation)).thenReturn(mockLocation);

        // when
        Location result = locationService.saveLocation(lat, lng);

        // then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(lat, result.getLatitude());
        Assertions.assertEquals(lng, result.getLongitude());

        Mockito.verify(reverseGeocodingService).reverseGeocode(lat, lng);
        Mockito.verify(locationRepository).save(mockLocation);
    }

    @Test
    void saveLocation_no_result_reverseGeocoding() {
        // given
        double lat = 0.0;
        double lng = 0.0;

        // mock 설정: reverseGeocode가 null 반환
        Mockito.when(reverseGeocodingService.reverseGeocode(lat, lng)).thenReturn(Mono.justOrEmpty(null));

        // when & then
        IllegalStateException ex = Assertions.assertThrows(
                IllegalStateException.class,
                () -> locationService.saveLocation(lat, lng)
        );

        Assertions.assertTrue(ex.getMessage().contains("Reverse geocoding failed"));
    }



}