package yeonjae.snapguide.service.util.googleMapApiDto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import yeonjae.snapguide.service.config.GoogleMapsConfig;

@Service
@Transactional
@RequiredArgsConstructor
public class PlaceAutocompleteService {
    private final GoogleMapsConfig googleMapsConfig;
    private final WebClient webClient = WebClient.create("https://maps.googleapis.com");
    // TODO : ReverseGeocodingService 처럼 예외 처리들 다 해줘야함
    /**
     * Google Places Autocomplete API를 호출하여 장소 추천 목록을 가져옵니다.
     * @param input 사용자가 입력한 텍스트
     * @return API 응답 결과 (JSON 문자열)
     */
    public String getPlaceAutocomplete(String input) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/maps/api/place/autocomplete/json")
                        .queryParam("input", input)
                        .queryParam("key", googleMapsConfig.getKey())
                        .queryParam("language", "ko")
                        .build()) // URI 빌드 종료
                .retrieve() // 요청 실행
                .bodyToMono(String.class) // 응답 바디를 Mono<String>으로 변환
                .block(); // 비동기 결과를 동기적으로 대기하며 String으로 반환 NOTE : 이렇게 하는게 올나? block() 이렇게 쓰는게 불안하네
    }

    /**
     * Google Places Details API를 호출하여 특정 장소의 좌표를 가져옵니다.
     * @param placeId 장소의 고유 ID
     * @return API 응답 결과 (JSON 문자열)
     */
    public String getPlaceDetails(String placeId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/maps/api/place/details/json")
                        .queryParam("place_id", placeId)
                        .queryParam("fields", "geometry") // 좌표 정보만 요청하여 비용 절감
                        .queryParam("key", googleMapsConfig.getKey())
                        .queryParam("language", "ko")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }


}
