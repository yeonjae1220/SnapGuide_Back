package yeonjae.snapguide.domain.location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {
//    private Double latitude;
//    private Double longitude;
    private Point coordinate;

    private String locationName;

    private String countryCode;
    private String formattedAddress;

    private String provider;

    private String country;
    private String region;
    private String city;
    private String subRegion;
    private String district;
    private String street;
    private String streetNumber;
    private String buildingName;
    private String subPremise;
    private String postalCode;

//    private String locale; // "ko", "en", "ja", ...
}
