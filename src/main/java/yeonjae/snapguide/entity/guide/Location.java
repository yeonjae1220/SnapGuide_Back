package yeonjae.snapguide.entity.guide;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor // (access = AccessLevel.PROTECTED)
@NoArgsConstructor
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {
                "country", "region", "subRegion", "locality", "route", "streetNumber", "premise", "subPremise"
        })
})
@ToString
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String locationName;
    private Double latitude;
    private Double longitude;
    /**
     * 우편 번호
     */
    // private String postalCode;

    private String country;
    /**
     * 광역 단위 (한국 도, 일본 현, 중국 성, 미국 주)
     * Tokyo, Gyeonggi-do, California
     */
    private String region;
    /**
     * 시/군/구
     * Shibuya-ku
     * Suwon-si
     * Los Angeles Country
     */
    private String subRegion;
    /**
     * 동/면/읍 등 소지역
     * Ebisu
     * Yeongtong-dong
     * Downtown LA
     */
    private String locality;

    /**
     * 거리 이름
     */
    private String route;
    /**
     * 번지/건물번호
     */
    private String streetNumber;
    /**
     * 건물 이름
     * googleplex, 롯데타워
     */
    private String premise;
    /**
     * 건물 안 가게 이름 같은 경우
     */
    private String subPremise;

//    public void assignGpsCoordinate(String locationName, Double latitude, Double longitude) {
//        this.locationName = locationName;
//        this.latitude = latitude;
//        this.longitude = longitude;
//    }
}
