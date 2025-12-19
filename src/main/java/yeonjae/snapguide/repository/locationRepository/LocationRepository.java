package yeonjae.snapguide.repository.locationRepository;

import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeonjae.snapguide.domain.location.Location;

import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Long>,  LocationRepositoryCustom{
//    boolean existsByLatitudeAndLongitude(double latitude, double longitude);
    boolean existsByCoordinate(Point coordinate);



    @Query(value = """
    SELECT *
    FROM location
    WHERE ST_DWithin(
        coordinate,
        ST_SetSRID(ST_MakePoint(:lng, :lat), 4326),
        0.00001
    )
""", nativeQuery = true)
    List<Location> findLocationByCoordinateNative(
            @Param("lat") Double lat, @Param("lng") Double lng
    );


    @Query(value = """
    SELECT * FROM location
    WHERE ST_DWithin(
        ST_Transform(coordinate, 3857),
        ST_Transform(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), 3857),
        :radius
    )
""", nativeQuery = true)
    List<Location> findNearbyNative(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radius") double radiusInMeters
    );

    /**
     * 주어진 위치 주변의 Location을 찾습니다.
     * Bounding Box + ST_DWithin을 사용하여 정확한 원형 범위 검색을 수행합니다.
     *
     * @param lat 중심 위도
     * @param lon 중심 경도
     * @param radiusInDegrees 반경 (degree 단위) - geometry 타입이므로 degree 사용
     * @param minLat Bounding Box 최소 위도
     * @param minLon Bounding Box 최소 경도
     * @param maxLat Bounding Box 최대 위도
     * @param maxLon Bounding Box 최대 경도
     * @return 범위 내 Location 목록
     */
    @Query(value = """
    SELECT * FROM location
    WHERE coordinate && ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)
      AND ST_DWithin(coordinate, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), :radius)
    """, nativeQuery = true)
    List<Location> findNearbyOptimized(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radius") double radiusInDegrees,
            @Param("minLat") double minLat,
            @Param("minLon") double minLon,
            @Param("maxLat") double maxLat,
            @Param("maxLon") double maxLon
    );


}
