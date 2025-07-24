package yeonjae.snapguide.service.mapService.googleMapService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.domain.location.LocationDto;
import yeonjae.snapguide.service.config.GoogleMapsConfig;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NearbyPlaceService {
    private final GoogleMapsConfig googleMapsConfig;
    private final WebClient webClient = WebClient.create("https://maps.googleapis.com");

    /**
     * 좌표 기준 가장 가까운 대표 장소 하나 가져오기
     */
    public Optional<LocationDto> getNearbyRepresentativePlace(double lat, double lng, int radius) {
        String apiKey = googleMapsConfig.getKey();

        String json = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/maps/api/place/nearbysearch/json")
                        .queryParam("location", lat + "," + lng)
                        .queryParam("radius", radius)
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("[NearbyPlaceService] Google API 응답 JSON: {}", json);

        return parseFirstPlace(json);
    }

    private Optional<LocationDto> parseFirstPlace(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode results = root.path("results");

            List<String> preferredTypes = List.of("point_of_interest", "establishment", "restaurant", "cafe", "museum");
            List<String> fallbackTypes = List.of("locality", "political");

            // ✅ 우선 preferredTypes 기준으로 검색
            for (JsonNode place : results) {
                if (hasMatchingType(place, preferredTypes)) {
                    return Optional.of(parseLocation(place));
                }
            }

            // ✅ fallbackTypes 기준으로 재검색
            for (JsonNode place : results) {
                if (hasMatchingType(place, fallbackTypes)) {
                    return Optional.of(parseLocation(place));
                }
            }

            return Optional.empty();
        } catch (Exception e) {
            log.warn("[NearbyPlaceService] JSON 파싱 실패", e);
            return Optional.empty();
        }
    }

    private boolean hasMatchingType(JsonNode place, List<String> typeList) {
        for (JsonNode typeNode : place.path("types")) {
            if (typeList.contains(typeNode.asText())) {
                return true;
            }
        }
        return false;
    }
    // TODO : 행정 구역 분류는 일단 불가, 한국만 한다면 가능하지만 여러 나라 고려했을 때 행정 구역 분류가 필요하다면 geocoding api 사용필요
    private LocationDto parseLocation(JsonNode place) {
        String name = place.path("name").asText(null);
        String address = place.path("vicinity").asText(null);
        String placeId = place.path("place_id").asText(null);
        double lat = place.path("geometry").path("location").path("lat").asDouble();
        double lng = place.path("geometry").path("location").path("lng").asDouble();

        return LocationDto.builder()
                .name(name)
                .address(address)
                .placeId(placeId)
                .latitude(lat)
                .longitude(lng)
                .build();
    }
}
