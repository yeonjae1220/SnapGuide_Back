package yeonjae.snapguide.service.util.googleMapApiDto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeocodingResultDto {
    @JsonProperty("formatted_address")
    private String formattedAddress;

    @JsonProperty("address_components")
    private List<AddressComponentDto> addressComponents;

    private GeometryDto geometry;
}
