package yeonjae.snapguide.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yeonjae.snapguide.entity.guide.Location;

public interface LocationRepository extends JpaRepository<Location, Long> {
}
