package yeonjae.snapguide.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import yeonjae.snapguide.domain.location.GeometryUtils;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.domain.location.LocationDto;
import yeonjae.snapguide.domain.location.LocationMapper;
import yeonjae.snapguide.service.config.GoogleMapsConfig;
import yeonjae.snapguide.service.util.googleMapApiDto.AddressComponentDto;
import yeonjae.snapguide.service.util.googleMapApiDto.GeocodingResponseDto;
import yeonjae.snapguide.service.util.googleMapApiDto.GeocodingResultDto;

import java.util.List;

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

                        // google geocoding api 사용량 초과 응답을 받을 경우 좌표 정보만 저장
                        String status = response.getStatus();
                        log.info("[ReverseGeocodingService] API status: {}, results count: {}",
                                status, response.getResults() == null ? 0 : response.getResults().size());

                        if (!"OK".equals(status) || CollectionUtils.isEmpty(response.getResults())) {
                            log.warn("[ReverseGeocodingService] API returned non-OK status or empty results: {}. Storing coordinates only.", status);
                            return Location.builder()
                                    .coordinate(GeometryUtils.createPoint(lat, lng))
                                    .build();
                        }


                        return response.getResults().stream()
                                .findFirst()
                                .map(result -> {
                                    LocationDto dto = buildDtoFromResult(result, lat, lng);
                                    return LocationMapper.toEntityWithJson(dto, json); // toEntity만 하다가 JSON 전체 추가 저장을 위해 이렇게 함
                                })
                                .orElse(null);

                    } catch (Exception e) {
                        log.error("Error during reverse geocoding", e);
                        return Location.builder()
//                                .latitude(lat)
//                                .longitude(lng)
                                .coordinate(GeometryUtils.createPoint(lat, lng))
                                .build();
                    }
                });
    }
    private LocationDto buildDtoFromResult(GeocodingResultDto result, double lat, double lng) {
        LocationDto.LocationDtoBuilder builder = LocationDto.builder()
                .formattedAddress(result.getFormattedAddress())
                .coordinate(GeometryUtils.createPoint(lat, lng))
                .provider("google");

        String region = null, city = null, subRegion = null, district = null;
        for (AddressComponentDto comp : result.getAddressComponents()) {
            List<String> types = comp.getTypes();
            if (types.contains("country"))
                builder.countryCode(comp.getShortName()).country(comp.getLongName());
            else if (types.contains("administrative_area_level_1")) { region    = comp.getLongName(); builder.region(region); }
            else if (types.contains("locality"))                    { city      = comp.getLongName(); builder.city(city); }
            else if (types.contains("administrative_area_level_2")) { subRegion = comp.getLongName(); builder.subRegion(subRegion); }
            else if (types.contains("sublocality_level_1"))         { subRegion = comp.getLongName(); builder.subRegion(subRegion); }
            else if (types.contains("sublocality_level_2"))         { district  = comp.getLongName(); builder.district(district); }
            else if (types.contains("sublocality_level_4"))   builder.street(comp.getLongName());
            else if (types.contains("street_number"))         builder.streetNumber(comp.getLongName());
            else if (types.contains("premise"))               builder.buildingName(comp.getLongName());
            else if (types.contains("subpremise"))            builder.subPremise(comp.getLongName());
            else if (types.contains("postal_code"))           builder.postalCode(comp.getLongName());
        }

        builder.locationName(buildLocationName(subRegion, district, city, region, result.getFormattedAddress()));
        return builder.build();
    }

    private String buildLocationName(String subRegion, String district, String city, String region, String formattedAddress) {
        if (subRegion != null && city != null) return subRegion + ", " + city;
        if (district  != null && city != null) return district  + ", " + city;
        if (city      != null && region != null) return city    + ", " + region;
        if (region    != null) return region;
        return formattedAddress;
    }

//    private Location buildLocationFromResult(GeocodingResultDto result, double lat, double lng) {
//        Location.LocationBuilder builder = Location.builder()
//                .locationName(result.getFormattedAddress())
//                .latitude(lat)
//                .longitude(lng);
//
//        log.info("[ReverseGeocodingService, buildLocationFromResult] : getAddressComponent: {}", result.getAddressComponents());
//
//        for (AddressComponentDto comp : result.getAddressComponents()) {
//            List<String> types = comp.getTypes();
//            log.info("[ReverseGeocodingService, buildLocationFromResult] : AddressComponent types: {}", comp.getTypes());
//            if (types.contains("country")) builder.country(comp.getLongName());
//            else if (types.contains("administrative_area_level_1")) builder.region(comp.getLongName());
//            else if (types.contains("administrative_area_level_2")) builder.subRegion(comp.getLongName());
//            else if (types.contains("locality")) builder.locality(comp.getLongName());
//            else if (types.contains("route")) builder.route(comp.getLongName());
//            else if (types.contains("street_number")) builder.streetNumber(comp.getLongName());
//            else if (types.contains("premise")) builder.premise(comp.getLongName());
//            else if (types.contains("subpremise")) builder.subPremise(comp.getLongName());
//        }
//
//        return builder.build();
//    }
}
