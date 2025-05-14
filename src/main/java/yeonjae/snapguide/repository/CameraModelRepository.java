package yeonjae.snapguide.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yeonjae.snapguide.entity.guide.CameraModel;

public interface CameraModelRepository extends JpaRepository<CameraModel, Long> {
}
