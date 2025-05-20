package yeonjae.snapguide.controller.locationController.locationDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationRequestDto {
    private double latitude;
    private double longitude;
}
