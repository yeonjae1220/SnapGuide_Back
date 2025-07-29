package yeonjae.snapguide.controller.initData;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.repository.locationRepository.LocationRepository;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LocationKrTourSpotTestData implements ApplicationRunner {
    private final LocationRepository locationRepository;
    private final ObjectMapper objectMapper;

    private boolean imported = true; // 다시 데이터가 들어가서 일단 임의로 막아둠

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (imported) return;

        try (InputStream is = new ClassPathResource("data/koreaTourSpot.json").getInputStream()) {
            TourSpotJsonWrapper wrapper = objectMapper.readValue(is, TourSpotJsonWrapper.class);
            List<TourSpotDto> spots = wrapper.getRecords();

            List<Location> newLocations = new ArrayList<>();
            for (TourSpotDto spot : spots) {
                boolean exists = locationRepository.existsByLatitudeAndLongitude(
                        spot.getLatitude(), spot.getLongitude());

                if (!exists) {
                    Location location = Location.builder()
                            .locationName(spot.getName())
                            .formattedAddress(spot.getRoadAddress())
                            .latitude(spot.getLatitude())
                            .longitude(spot.getLongitude())
                            .rawJson(objectMapper.writeValueAsString(spot))
                            .provider("행정안전부")
                            .build();

                    newLocations.add(location);
                }
            }

            locationRepository.saveAll(newLocations);
            log.info("✅ 관광지 {}건 import 완료", newLocations.size());
            imported = true;

        } catch (Exception e) {
            log.error("❌ 관광지 import 실패", e);
        }
    }
}
