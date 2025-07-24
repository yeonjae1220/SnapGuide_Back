package yeonjae.snapguide.domain.location;

public class LocationMapper {
    public static Location toEntity(LocationDto dto) {
        return Location.builder()
//                .placeId(dto.getPlaceId())
//                .name(dto.getName())
//                .address(dto.getAddress())
                .district(dto.getDistrict())
                .region(dto.getRegion())
                .countryCode(dto.getCountryCode())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .build();
    }

    public static Location toEntity(LocationReverseGeoDto dto) {
        return Location.builder()
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .countryCode(dto.getCountryCode())
                .formattedAddress(dto.getFormattedAddress())
                .country(dto.getCountry())
                .region(dto.getRegion())
                .city(dto.getCity())
                .subRegion(dto.getSubRegion())
                .district(dto.getDistrict())
                .street(dto.getStreet())
                .streetNumber(dto.getStreetNumber())
                .buildingName(dto.getBuildingName())
                .subPremise(dto.getSubPremise())
                .postalCode(dto.getPostalCode())
                .build();
    }


    public static LocationDto toDto(Location entity) {
        return LocationDto.builder()
//                .placeId(entity.getPlaceId())
//                .name(entity.getName())
//                .address(entity.getAddress())
                .district(entity.getDistrict())
                .region(entity.getRegion())
                .countryCode(entity.getCountryCode())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .build();
    }








//    public static Location toEntity(LocationDto dto) {
//        return Location.builder()
//                .latitude(dto.getLatitude())
//                .longitude(dto.getLongitude())
//                .countryCode(dto.getCountryCode())
//                .formattedAddress(dto.getFormattedAddress())
//                .country(dto.getCountry())
//                .region(dto.getRegion())
//                .city(dto.getCity())
//                .subRegion(dto.getSubRegion())
//                .district(dto.getDistrict())
//                .street(dto.getStreet())
//                .streetNumber(dto.getStreetNumber())
//                .buildingName(dto.getBuildingName())
//                .subPremise(dto.getSubPremise())
//                .postalCode(dto.getPostalCode())
////                .locale(dto.getLocale())
//                .build();
//    }
//
//    public static LocationDto toDto(Location entity) {
//        return LocationDto.builder()
//                .latitude(entity.getLatitude())
//                .longitude(entity.getLongitude())
//                .countryCode(entity.getCountryCode())
//                .formattedAddress(entity.getFormattedAddress())
//                .country(entity.getCountry())
//                .region(entity.getRegion())
//                .city(entity.getCity())
//                .subRegion(entity.getSubRegion())
//                .district(entity.getDistrict())
//                .street(entity.getStreet())
//                .streetNumber(entity.getStreetNumber())
//                .buildingName(entity.getBuildingName())
//                .subPremise(entity.getSubPremise())
//                .postalCode(entity.getPostalCode())
////                .locale(entity.getLocale())
//                .build();
//    }

}
