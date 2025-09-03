package yeonjae.snapguide.service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter // TODO: 얘 안쓸 방법 찾아보자. 그냥 Setter가 나는 싫다
@Configuration // HACK : 이거 말고 그냥 가볍게 @Component만?
@ConfigurationProperties(prefix = "google.maps.api")
public class GoogleMapsConfig {
    private String key;
}

/**
 *
 * ReverseGeocodingService에 이 클래스 말고
 * @Value("${google.maps.api.key:}")
 *     private String apiKey;  // 끝에 : 추가해서 값이 없을 경우 빈 문자열 기본값, Lombok @Value와 임포트 혼동 주의
 *
 *     바로 이 코드를 넣을 수 도 있지만, 실무에서는 환경설정을 별도 Configuration Properties 클래스로 분리하여 처리합니다.
 *     라고 한다. 더 안전하고 테스트 가능한 구조라나
 */


/**
 * application.properties의 'google.maps.api' 설정을 담는 불변(immutable) 객체.
 * Java record를 사용하면 final 필드, 생성자, getter, equals, hashCode, toString이 자동으로 생성됩니다.
 * Setter가 없으므로 @Setter 어노테이션이 필요 없습니다.
 */