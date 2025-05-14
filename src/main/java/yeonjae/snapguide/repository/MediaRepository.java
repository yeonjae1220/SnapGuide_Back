package yeonjae.snapguide.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yeonjae.snapguide.entity.guide.Media;

public interface MediaRepository extends JpaRepository<Media, Long>, MediaRepositoryCustom {
}
