package yeonjae.snapguide.repository.mediaRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeonjae.snapguide.domain.media.Media;

import java.util.List;

public interface MediaRepository extends JpaRepository<Media, Long>, MediaRepositoryCustom {
    @Query("SELECT l.id FROM Media m JOIN m.location l WHERE m.id IN :mediaIds AND l IS NOT NULL")
    List<Long> findFirstLocationIdByMediaIds(@Param("mediaIds") List<Long> mediaIds, Pageable pageable);
}
