package yeonjae.snapguide.service.map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.service.config.GoogleMapsConfig;

import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class MapService {
    // 🔹 @Value 대신 ConfigurationProperties 객체를 직접 주입받습니다.
    private final GoogleMapsConfig googleMapsConfig;

    /**
     * 주입받은 설정 객체에서 API 키를 가져와 Map 형태로 반환합니다.
     * @return API 키가 담긴 Map 객체
     */
    public Map<String, String> getGoogleMapsApiKey() {
        // googleMapsConfig.key()는 record의 getter입니다.
        // 일반 클래스였다면 googleMapsConfig.getKey()
        return Map.of("apiKey", googleMapsConfig.getKey());
    }
}
