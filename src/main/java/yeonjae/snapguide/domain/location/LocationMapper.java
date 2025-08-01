package yeonjae.snapguide.domain.location;

public class LocationMapper {
    public static Location toEntity(LocationDto dto) {
        return Location.builder()
//                .latitude(dto.getLatitude())
//                .longitude(dto.getLongitude())
                .coordinate(dto.getCoordinate())
                .locationName(dto.getLocationName())
                .countryCode(dto.getCountryCode())
                .formattedAddress(dto.getFormattedAddress())
                .provider(dto.getProvider())
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
//                .locale(dto.getLocale())
                .build();
    }

    public static Location toEntityWithJson(LocationDto dto, String rawJson) {
        return Location.builder()
//                .latitude(dto.getLatitude())
//                .longitude(dto.getLongitude())
                .coordinate(dto.getCoordinate())
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
//                .locale(dto.getLocale())
                .rawJson(rawJson)
                .build();
    }

    public static LocationDto toDto(Location entity) {
        return LocationDto.builder()
//                .latitude(entity.getLatitude())
//                .longitude(entity.getLongitude())
                .coordinate(entity.getCoordinate())
                .countryCode(entity.getCountryCode())
                .formattedAddress(entity.getFormattedAddress())
                .country(entity.getCountry())
                .region(entity.getRegion())
                .city(entity.getCity())
                .subRegion(entity.getSubRegion())
                .district(entity.getDistrict())
                .street(entity.getStreet())
                .streetNumber(entity.getStreetNumber())
                .buildingName(entity.getBuildingName())
                .subPremise(entity.getSubPremise())
                .postalCode(entity.getPostalCode())
//                .locale(entity.getLocale())
                .build();
    }

}
