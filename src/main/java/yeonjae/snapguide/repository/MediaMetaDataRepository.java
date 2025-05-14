package yeonjae.snapguide.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yeonjae.snapguide.entity.guide.MediaMetaData;

public interface MediaMetaDataRepository extends JpaRepository<MediaMetaData, Long> {
}
