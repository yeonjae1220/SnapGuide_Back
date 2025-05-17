package yeonjae.snapguide.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import yeonjae.snapguide.entity.guide.Location;
import yeonjae.snapguide.entity.guide.mediaUtil.exifUtil.extrator.ExifCoordinateExtractor;

import java.io.File;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private ReverseGeocodingService reverseGeocodingService;
    @InjectMocks
    private LocationService locationService;

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

            // when
            Location location = locationService.extractAndResolveLocation(dummyFile);

            // then
            assertNotNull(location);
            assertEquals("서울특별시", location.getLocationName());
        }
    }
}