package yeonjae.snapguide.repository.mediaMetaDataRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import yeonjae.snapguide.domain.mediaMetaData.MediaMetaData;

public interface MediaMetaDataRepository extends JpaRepository<MediaMetaData, Long> {
}
