package yeonjae.snapguide.controller.admin.dto;

import lombok.Builder;
import lombok.Getter;
import yeonjae.snapguide.domain.location.Location;

@Getter
@Builder
public class AdminLocationResponse {
    private Long id;
    private String locationName;
    private String country;
    private String region;
    private String city;
    private String formattedAddress;

    public static AdminLocationResponse of(Location location) {
        return AdminLocationResponse.builder()
                .id(location.getId())
                .locationName(location.getLocationName())
                .country(location.getCountry())
                .region(location.getRegion())
                .city(location.getCity())
                .formattedAddress(location.getFormattedAddress())
                .build();
    }
}
