package yeonjae.snapguide.infrastructure.RuntimeTest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.infrastructure.aop.TimeTrace;
import yeonjae.snapguide.repository.locationRepository.GeoUtil;
import yeonjae.snapguide.repository.locationRepository.LocationRepository;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuntimeTestService {
    private final LocationRepository locationRepository ;

    private record LocationPoint(String name, double lat, double lng) {}

    private final List<LocationPoint> testLocations = List.of(
            new LocationPoint("New York", Math.toDegrees(0.6068093722690607), Math.toDegrees(0.7094027578544309)),
            new LocationPoint("Tokyo", Math.toDegrees(0.6194755291745206), Math.toDegrees(2.325462150946811)),
            new LocationPoint("Saint Petersburg", Math.toDegrees(0.9726429847642717), Math.toDegrees(0.6569802578810394)),
            new LocationPoint("Shanghai", Math.toDegrees(0.7935381851032774), Math.toDegrees(1.4827008328004827)),
            new LocationPoint("Busan", 35.09661444, 129.0305818)
//            new LocationPoint("Pacific ocean", 8.573916, -171.641706) // 등록 안되어 있는 위치
    );

    private final List<Double> radiusKmList = List.of(1.0, 5.0, 10.0, 50.0);

    @TimeTrace
    @Transactional
    public void testExactCoordinateSearch() {
        log.info("=== Start Exact Coordinate Search Test ===");
        StopWatch watch = new StopWatch("CoordinateSearchSummary");
        for (RuntimeTestService.LocationPoint location : testLocations) {
            watch.start(location.name());

            List<Location> result = locationRepository.findLocationByCoordinateNative(location.lat(), location.lng);

            logLocations(location.name(), result);
            watch.stop();
            log.info("📍 [{}] coordinate matched", location.name());
        }
        log.info("\n{}", watch.prettyPrint());  // 결과 한 번에 출력
    }

    @TimeTrace
    @Transactional
    public void testSquareSearch() {
        log.info("=== Start Square Search Test ===");
        StopWatch watch = new StopWatch("SquareSearchSummary");
        for (LocationPoint location : testLocations) {
            for (Double radius : radiusKmList) {
                String taskName = String.format("%s - Square %.0fkm", location.name(), radius);
                watch.start(taskName);
                List<Location> result = locationRepository.findWithinSquare(location.lat(), location.lng, radius);
                logLocations(location.name(), result);
                watch.stop();
                log.info("🟦 [{}] square radius {}km matched", location.name(), radius);
            }
        }
        log.info("\n{}", watch.prettyPrint());
    }

    @TimeTrace
    @Transactional
    public void testRadiusSearch() {
        log.info("=== Start Radius (Haversine) Search Test ===");
        StopWatch watch = new StopWatch("RadiusSearchSummary");
        for (LocationPoint location : testLocations) {
            for (Double radius : radiusKmList) {
                String taskName = String.format("%s - Radius %.0fkm", location.name(), radius);
                watch.start(taskName);
                List<Location> result = locationRepository.findWithinRadius(location.lat(), location.lng(), radius);
                logLocations(location.name(), result);
                watch.stop();
                log.info("🟢 [{}] radius {}km matched", location.name(), radius);
            }
        }
        log.info("\n{}", watch.prettyPrint());
    }

    @TimeTrace
    @Transactional
    public void testOptimizedSearch() {
        log.info("=== Start BoundingBox + Java Haversine Filter Test ===");
        StopWatch watch = new StopWatch("OptimizedSearchSummary");
        for (LocationPoint location : testLocations) {
            for (Double radius : radiusKmList) {
                String taskName = String.format("%s - Optimized %.0fkm", location.name(), radius);
                watch.start(taskName);
                List<Location> result =  locationRepository.findNearby(location.lat(), location.lng(), radius);
                logLocations(location.name(), result);
                watch.stop();
                log.info("✅ [{}] optimized radius {}km matched", location.name(), radius);
            }
        }
        log.info("\n{}", watch.prettyPrint());
    }

    @TimeTrace
    @Transactional
    public void findNearbyNative() {
        log.info("=== findNearByNative + Java Haversine Filter Test ===");
        StopWatch watch = new StopWatch("OptimizedSearchSummary");
        for (LocationPoint location : testLocations) {
            for (Double radius : radiusKmList) {
                String taskName = String.format("%s - Optimized %.0fkm", location.name(), radius);
                watch.start(taskName);
                List<Location> result =  locationRepository.findNearbyNative(location.lat(), location.lng(), radius * 1000);
                logLocations(location.name(), result);
                watch.stop();
                log.info("✅ [{}] optimized radius {}km matched", location.name(), radius);
            }
        }
        log.info("\n{}", watch.prettyPrint());
    }



    @TimeTrace
    @Transactional
    public void findNearbyOptimized() {
        log.info("=== findNearbyOptimized + Java Haversine Filter Test ===");
        StopWatch watch = new StopWatch("OptimizedSearchSummary");
        for (LocationPoint location : testLocations) {
            for (Double radius : radiusKmList) {
                String taskName = String.format("%s - Optimized %.0fkm", location.name(), radius);
                watch.start(taskName);
                double[] box = GeoUtil.getBoundingBox(location.lat(), location.lng(), radius);
                double minLat = box[0], maxLat = box[1];
                double minLng = box[2], maxLng = box[3];
                List<Location> result =  locationRepository.findNearbyOptimized(location.lat(), location.lng(), radius * 1000,
                        minLat, minLng, maxLat, maxLng);
                logLocations(location.name(), result);
                watch.stop();
                log.info("✅ [{}] optimized radius {}km matched", location.name(), radius);
            }
        }
        log.info("\n{}", watch.prettyPrint());
    }

    private void logLocations(String context, List<Location> locations) {
        for (Location loc : locations) {
            Point point = loc.getCoordinate();
            log.info("🔍 [{}] {} - Coordinate: (lat={}, lng={})", context, loc.getLocationName(), point.getY(), point.getX());
        }
    }
}
