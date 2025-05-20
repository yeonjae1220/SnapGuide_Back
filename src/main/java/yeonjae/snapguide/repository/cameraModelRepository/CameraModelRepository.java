package yeonjae.snapguide.repository.cameraModelRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import yeonjae.snapguide.domain.cameraModel.CameraModel;

public interface CameraModelRepository extends JpaRepository<CameraModel, Long> {
}
