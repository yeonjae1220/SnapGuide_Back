package yeonjae.snapguide.repository.locationRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import yeonjae.snapguide.domain.location.Location;

public interface LocationRepository extends JpaRepository<Location, Long> {
}
