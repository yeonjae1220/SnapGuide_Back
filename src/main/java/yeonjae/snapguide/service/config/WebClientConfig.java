package yeonjae.snapguide.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.client.RestTemplate;

@Configuration // HACK : 이거 말고 그냥 가볍게 @Component만?
public class WebClientConfig {
    @Bean
    public WebClient googleMapsWebClient(GoogleMapsConfig config) {
        return WebClient.builder()
                .baseUrl("https://maps.googleapis.com/maps/api/geocode")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) // MediaTyoe 클래스 만들어 둔거랑 이름 겹침
                .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

/**
 * 이럴 필요 없이 그냥 final 로 주입함
 */