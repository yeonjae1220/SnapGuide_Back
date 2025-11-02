package yeonjae.snapguide.domain.location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {
    private Double latitude;
    private Double longitude;

    private String placeId;
    private String name;
    private String address;
    private String district;
    private String region;
    private String countryCode;

}
