package yeonjae.snapguide.domain.location;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor // (access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double latitude;
    private Double longitude;

    private String placeId;             // Google Place ID
    private String name;                // 장소 이름
    private String address;             // 전체 주소 formattedAddress
    private String district;            // 행정 동
    private String region;              // 시/도 단위
    private String countryCode;         // KR, JP 등



//    // 국가 코드 (예: "KR", "JP", "US")
//    private String countryCode;
//    // 전체 포맷 주소 (Google Maps의 formatted_address)
//    private String formattedAddress;
//
//    private String country;
//    /**ㅈ
//     * 광역 단위 (한국 도, 일본 현, 중국 성, 미국 주)
//     * Tokyo, Gyeonggi-do, California
//     */
//    private String region;
//    /**
//     * 시
//     * Suwon-si
//     * Hamamatsu-si
//     */
//    private String city;
//
//    /**
//     * 군/구
//     * Shibuya-ku
//     * Los Angeles Country ?
//     */
//    private String subRegion;
//
//    /**
//     * 동/면/읍 등 소지역
//     * Ebisu
//     * Yeongtong-dong
//     * Downtown LA
//     */
//    private String district;
//
//    /**
//     * 거리 이름
//     */
//    private String street;
//    /**
//     * 번지/건물번호
//     */
//    private String streetNumber;
//    /**
//     * 건물 이름
//     * googleplex, 롯데타워
//     */
//    private String buildingName;
//    /**
//     * 건물 안 가게 이름 같은 경우, 방 번호 등
//     */
//    private String subPremise;
//
//    /**
//     * 우편번호
//     */
//    private String postalCode;
//
//    // 사용자의 언어 또는 출력용 주소 언어
////    private String locale;        // "ko", "en", "ja", ...

}
