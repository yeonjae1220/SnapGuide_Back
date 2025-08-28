package yeonjae.snapguide.repository.locationRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.domain.location.Location;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Transactional
class LocationRepositoryCustomImplTest {
    @Autowired
    LocationRepository locationRepository;

    @Test
    public void find_location_in_db() {
        List<Location> location1 = locationRepository.findLocationByCoordinateNative(37.5, 127.0);
        List<Location> location2 = locationRepository.findLocationByCoordinateNative(35.09661444, 129.0305818); // 실제 있는 데이터

        List<Location> location3 = locationRepository.findLocationByCoordinate(37.5, 127.0);
        List<Location> location4 = locationRepository.findLocationByCoordinate(35.09661444, 129.0305818); // 실제 있는 데이터

        System.out.println("=== location1 ===");
        for (Location loc : location1) {
            System.out.printf("ID: %d, LAT: %.6f, LNG: %.6f%n",
                    loc.getId(),
                    loc.getCoordinate().getY(), // 위도
                    loc.getCoordinate().getX()); // 경도
        }

        System.out.println("=== location2 ===");
        for (Location loc : location2) {
            System.out.printf("ID: %d, LAT: %.6f, LNG: %.6f%n",
                    loc.getId(),
                    loc.getCoordinate().getY(),
                    loc.getCoordinate().getX());
        }

        System.out.println("=== location3 ===");
        for (Location loc : location3) {
            System.out.printf("ID: %d, LAT: %.6f, LNG: %.6f%n",
                    loc.getId(),
                    loc.getCoordinate().getY(),
                    loc.getCoordinate().getX());
        }

        System.out.println("=== location4 ===");
        for (Location loc : location4) {
            System.out.printf("ID: %d, LAT: %.6f, LNG: %.6f%n",
                    loc.getId(),
                    loc.getCoordinate().getY(),
                    loc.getCoordinate().getX());
        }

        Location findLoc1 = locationRepository.findById(425060l).get();
        Location findLoc2 = locationRepository.findById(425090l).get();
        Location findLoc3 = locationRepository.findById(425120l).get();

        System.out.println("=== findLoc1 ===");
        System.out.printf("NAME: %s, LAT: %f, LNG: %f%n",
                findLoc1.getLocationName(),
                findLoc1.getCoordinate().getY(),
                findLoc1.getCoordinate().getX());
        System.out.println("=== findLoc2 ===");
        System.out.printf("NAME: %s, LAT: %f, LNG: %f%n",
                findLoc2.getLocationName(),
                findLoc2.getCoordinate().getY(),
                findLoc2.getCoordinate().getX());
        System.out.println("=== findLoc3 ===");
        System.out.printf("NAME: %s, LAT: %f, LNG: %f%n",
                findLoc3.getLocationName(),
                findLoc3.getCoordinate().getY(),
                findLoc3.getCoordinate().getX());
    }

}