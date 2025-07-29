package yeonjae.snapguide.controller.initData;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourSpotJsonWrapper {
    private List<TourSpotDto> records;
}
