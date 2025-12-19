package yeonjae.snapguide.repository.locationRepository;

public class GeoUtil {
    private static final double EARTH_RADIUS_KM = 6371.0;

    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(dLon / 2), 2);

        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }

    public static double[] getBoundingBox(double lat, double lon, double radiusKm) {
        double deltaLat = Math.toDegrees(radiusKm / EARTH_RADIUS_KM);
        double deltaLon = Math.toDegrees(radiusKm / (EARTH_RADIUS_KM * Math.cos(Math.toRadians(lat))));

        double minLat = lat - deltaLat;
        double maxLat = lat + deltaLat;
        double minLon = lon - deltaLon;
        double maxLon = lon + deltaLon;

        return new double[]{minLat, maxLat, minLon, maxLon};
    }

    /**
     * km 반경을 degree로 변환 (ST_DWithin에서 geometry 타입 사용 시)
     *
     * @param lat 중심점의 위도
     * @param radiusKm 반경 (km)
     * @return 반경 (degree)
     */
    public static double kmToDegrees(double lat, double radiusKm) {
        // 위도 1도 ≈ 111km (고정)
        double latDegrees = radiusKm / 111.0;

        // 경도 1도 = 111km * cos(위도)
        // 위도가 높을수록 경도 간 거리가 짧아짐
        double lonDegrees = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

        // 원을 완전히 포함하려면 둘 중 큰 값 사용
        return Math.max(latDegrees, lonDegrees);
    }
}
