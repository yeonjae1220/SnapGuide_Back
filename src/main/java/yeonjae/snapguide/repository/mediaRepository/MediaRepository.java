package yeonjae.snapguide.repository.mediaRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import yeonjae.snapguide.domain.media.Media;

public interface MediaRepository extends JpaRepository<Media, Long>, MediaRepositoryCustom {
}
