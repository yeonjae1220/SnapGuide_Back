package yeonjae.snapguide.service;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import yeonjae.snapguide.service.config.GoogleMapsConfig;

@Service
@RequiredArgsConstructor
public class ReverseGeocodingService {
    private final GoogleMapsConfig googleMapsConfig;
    private final WebClient webClient = WebClient.create("https://maps.googleapis.com");

    public Mono<String> reverseGeocode(double lat, double lng) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/maps/api/geocode/json")
                        .queryParam("latlng", lat + "," + lng)
                        .queryParam("key", googleMapsConfig.getKey())
                        .build())
                .retrieve()
                .bodyToMono(String.class); // 또는 원하는 DTO로 변환 가능
    }
}
