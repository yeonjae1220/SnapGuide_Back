package yeonjae.snapguide.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "google.maps.api")
public class GoogleMapsConfig {
    private final String key;
    private final String geocodingKey;

    @ConstructorBinding
    public GoogleMapsConfig(String key, String geocodingKey) {
        this.key = key;
        this.geocodingKey = geocodingKey;
    }

    public String getKey() {
        return key;
    }

    public String getGeocodingKey() {
        return geocodingKey;
    }
}