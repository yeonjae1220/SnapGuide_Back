package yeonjae.snapguide.service.mapService.googleMapService;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.domain.location.LocationDto;
import yeonjae.snapguide.domain.location.LocationMapper;
import yeonjae.snapguide.domain.location.LocationReverseGeoDto;
import yeonjae.snapguide.service.config.GoogleMapsConfig;
import yeonjae.snapguide.service.util.googleMapApiDto.AddressComponentDto;
import yeonjae.snapguide.service.util.googleMapApiDto.GeocodingResponseDto;
import yeonjae.snapguide.service.util.googleMapApiDto.GeocodingResultDto;

import java.util.List;
// NOTE : ToEntity 함수 미완성임, 이 코드 살려만 놓고 사용은 당장 안할꺼라 추후 정리 필요
@Service
@RequiredArgsConstructor
@Slf4j
public class ReverseGeocodingService {
    private final GoogleMapsConfig googleMapsConfig;
    private final WebClient webClient = WebClient.create("https://maps.googleapis.com");

    public Mono<Location> reverseGeocode(double lat, double lng) {
        log.info("[ReverseGeocodingService, reverseGeocode] : start");
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/maps/api/geocode/json")
                        .queryParam("latlng", lat + "," + lng)
                        .queryParam("key", googleMapsConfig.getKey())
                        .queryParam("language", "en")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(json -> {
                    log.info("[ReverseGeocodingService] Google API 응답 JSON: {}", json);
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        GeocodingResponseDto response = mapper.readValue(json, GeocodingResponseDto.class);

                        log.info("[ReverseGeocodingService, reverseGeocode] : working");

                        return response.getResults().stream()
                                .findFirst()
                                .map(result -> {
                                    LocationReverseGeoDto dto = buildDtoFromResult(result, lat, lng);
                                    return LocationMapper.toEntity(dto);
                                })
                                .orElse(null);

                    } catch (Exception e) {
                        // TODO : 로그로
                        log.info("[ReverseGeocodingService, reverseGeocode] : exception catched");
                        e.printStackTrace();
                        return null;
                    }
                });
    }
// 우선 한국용
    private LocationReverseGeoDto buildDtoFromResult(GeocodingResultDto result, double lat, double lng) {
        LocationReverseGeoDto.LocationReverseGeoDtoBuilder builder = LocationReverseGeoDto.builder()
                .formattedAddress(result.getFormattedAddress())
                .latitude(lat)
                .longitude(lng);
//                .locale(locale);

        for (AddressComponentDto comp : result.getAddressComponents()) {
            List<String> types = comp.getTypes();
            if (types.contains("country")) builder.countryCode(comp.getShortName()).country(comp.getLongName());
            else if (types.contains("administrative_area_level_1")) builder.region(comp.getLongName());
            else if (types.contains("locality")) builder.city(comp.getLongName()); // 일본에서 시, 그리고 karuizawa같은 마치
            else if (types.contains("administrative_area_level_2")) builder.subRegion(comp.getLongName()); // 일본에서 구가 여기 들어가는 듯 함
            else if (types.contains("sublocality_level_1")) builder.subRegion(comp.getLongName());
            else if (types.contains("sublocality_level_2")) builder.district(comp.getLongName());
//            else if (types.contains("route")) builder.street(comp.getLongName());
            else if (types.contains("sublocality_level_4")) builder.street(comp.getLongName());
            else if (types.contains("street_number")) builder.streetNumber(comp.getLongName());
            else if (types.contains("premise")) builder.buildingName(comp.getLongName());
            else if (types.contains("subpremise")) builder.subPremise(comp.getLongName());
            else if (types.contains("postal_code")) builder.postalCode(comp.getLongName());
        }

        return builder.build();
    }

}
