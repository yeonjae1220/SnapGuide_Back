package yeonjae.snapguide.controller.initData;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourSpotDto {
    @JsonProperty("관광지명")
    private String name;

    @JsonProperty("관광지구분")
    private String category;

    @JsonProperty("소재지도로명주소")
    private String roadAddress;

    @JsonProperty("소재지지번주소")
    private String parcelAddress;

    @JsonProperty("위도")
    private double latitude;

    @JsonProperty("경도")
    private double longitude;

    @JsonProperty("면적")
    private String area;

    @JsonProperty("공공편익시설정보")
    private String publicFacilities;

    @JsonProperty("숙박시설정보")
    private String lodgingFacilities;

    @JsonProperty("운동및오락시설정보")
    private String entertainmentFacilities;

    @JsonProperty("휴양및문화시설정보")
    private String leisureFacilities;

    @JsonProperty("접객시설정보")
    private String receptionFacilities;

    @JsonProperty("지원시설정보")
    private String supportFacilities;

    @JsonProperty("지정일자")
    private String designationDate;

    @JsonProperty("수용인원수")
    private String capacity;

    @JsonProperty("주차가능수")
    private String parkingSpaces;

    @JsonProperty("관광지소개")
    private String description;

    @JsonProperty("관리기관전화번호")
    private String agencyPhone;

    @JsonProperty("관리기관명")
    private String agencyName;

    @JsonProperty("데이터기준일자")
    private String referenceDate;

    @JsonProperty("제공기관코드")
    private String providerCode;

    @JsonProperty("제공기관명")
    private String providerName;
}
