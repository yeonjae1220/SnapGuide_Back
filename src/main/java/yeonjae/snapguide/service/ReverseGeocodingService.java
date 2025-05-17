package yeonjae.snapguide.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import yeonjae.snapguide.entity.guide.Location;
import yeonjae.snapguide.service.config.GoogleMapsConfig;
import yeonjae.snapguide.service.util.googleMapApiDto.AddressComponentDto;
import yeonjae.snapguide.service.util.googleMapApiDto.GeocodingResponseDto;
import yeonjae.snapguide.service.util.googleMapApiDto.GeocodingResultDto;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReverseGeocodingService {
    private final GoogleMapsConfig googleMapsConfig;
    private final WebClient webClient = WebClient.create("https://maps.googleapis.com");

    public Mono<Location> reverseGeocode(double lat, double lng) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/maps/api/geocode/json")
                        .queryParam("latlng", lat + "," + lng)
                        .queryParam("key", googleMapsConfig.getKey())
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(json -> {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        GeocodingResponseDto response = mapper.readValue(json, GeocodingResponseDto.class);

                        return response.getResults().stream()
                                .findFirst()
                                .map(result -> buildLocationFromResult(result, lat, lng))
                                .orElse(null);

                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                });
    }

    private Location buildLocationFromResult(GeocodingResultDto result, double lat, double lng) {
        Location.LocationBuilder builder = Location.builder()
                .locationName(result.getFormattedAddress())
                .latitude(lat)
                .longitude(lng);

        for (AddressComponentDto comp : result.getAddressComponents()) {
            List<String> types = comp.getTypes();
            if (types.contains("country")) builder.country(comp.getLongName());
            else if (types.contains("administrative_area_level_1")) builder.region(comp.getLongName());
            else if (types.contains("administrative_area_level_2")) builder.subRegion(comp.getLongName());
            else if (types.contains("locality")) builder.locality(comp.getLongName());
            else if (types.contains("route")) builder.route(comp.getLongName());
            else if (types.contains("street_number")) builder.streetNumber(comp.getLongName());
            else if (types.contains("premise")) builder.premise(comp.getLongName());
            else if (types.contains("subpremise")) builder.subPremise(comp.getLongName());
        }

        return builder.build();
    }
}
