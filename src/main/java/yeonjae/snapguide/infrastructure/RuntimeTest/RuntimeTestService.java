package yeonjae.snapguide.infrastructure.RuntimeTest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;
import yeonjae.snapguide.infrastructure.aop.TimeTrace;
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
            locationRepository.findLocationByCoordinate(location.lat(), location.lng);
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
                locationRepository.findWithinSquare(location.lat(), location.lng, radius);
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
                locationRepository.findWithinRadius(location.lat(), location.lng(), radius);
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
                locationRepository.findNearby(location.lat(), location.lng(), radius);
                watch.stop();
                log.info("✅ [{}] optimized radius {}km matched", location.name(), radius);
            }
        }
        log.info("\n{}", watch.prettyPrint());
    }
}
