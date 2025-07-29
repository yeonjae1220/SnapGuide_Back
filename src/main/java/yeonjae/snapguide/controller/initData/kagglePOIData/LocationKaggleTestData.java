package yeonjae.snapguide.controller.initData.kagglePOIData;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.bean.CsvToBeanBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.repository.locationRepository.LocationRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class LocationKaggleTestData implements ApplicationRunner {

    private final LocationRepository locationRepository;
    private final ObjectMapper objectMapper;

    // 한 번만 실행되도록 설정
    private static boolean imported = true; // 다시 데이터가 들어가서 일단 임의로 막아둠

    @Override
    public void run(ApplicationArguments args) {
        if (imported) return;

        try (InputStream inputStream = new ClassPathResource("data/poi.csv").getInputStream();
             InputStreamReader reader = new InputStreamReader(inputStream)) {

            List<POICsvRow> rows = new CsvToBeanBuilder<POICsvRow>(reader)
                    .withType(POICsvRow.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();

            // 중복 필터링 기준: address + provider
            Set<String> existingKeys = locationRepository.findAll().stream()
                    .map(loc -> loc.getFormattedAddress() + "::" + loc.getProvider())
                    .collect(Collectors.toSet());

            List<Location> newLocations = new ArrayList<>();

            for (POICsvRow row : rows) {
                double latDeg = Math.toDegrees(row.getLatitudeRadian());
                double lngDeg = Math.toDegrees(row.getLongitudeRadian());
                String address = row.getName();
                String provider = "kaggle";
                String key = address + "::" + provider;

                if (existingKeys.contains(key)) continue;

                String rawJson = objectMapper.writeValueAsString(row);

                Location location = Location.builder()
                        .latitude(latDeg)
                        .longitude(lngDeg)
                        .provider(provider)
                        .formattedAddress(address)
                        .rawJson(rawJson)
                        .build();

                newLocations.add(location);
            }

            if (!newLocations.isEmpty()) {
                locationRepository.saveAll(newLocations);
                log.info("✅ {} new locations imported from CSV", newLocations.size());
            } else {
                log.info("ℹ️ No new locations to import (all are duplicates)");
            }

            imported = true;

        } catch (Exception e) {
            log.error("❌ CSV import failed", e);
        }
    }
}

/*
    •	save()를 루프마다 호출하는 대신, saveAll()으로 배치 저장.
	•	현재는 메모리에 Set으로 formattedAddress + provider 조합을 유지해 체크하고 있어 수천~수만 건이면 충분히 빠릅니다.
	•	데이터가 수십만 건으로 늘어난다면, 쿼리 방식으로 체크하거나, @Column(unique = true)를 설정하고 try-catch로 중복 예외를 처리하는 전략도 가능합니다.
 */