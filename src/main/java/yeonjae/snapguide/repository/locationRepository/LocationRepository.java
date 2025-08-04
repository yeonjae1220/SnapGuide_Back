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

    @Query(value = """
    SELECT * FROM location
    WHERE coordinate && ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)
      AND ST_DWithin(coordinate, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), :radius)
    """, nativeQuery = true)
    List<Location> findNearbyOptimized(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radius") double radiusInMeters,
            @Param("minLat") double minLat,
            @Param("minLon") double minLon,
            @Param("maxLat") double maxLat,
            @Param("maxLon") double maxLon
    );


}
