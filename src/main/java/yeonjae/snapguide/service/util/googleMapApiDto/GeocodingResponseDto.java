package yeonjae.snapguide.service.util.googleMapApiDto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // Plus_code 없으니 에러 발생
public class GeocodingResponseDto {
    private List<GeocodingResultDto> results;
    private String status;
    // private Map<String, String> plus_code;
}
