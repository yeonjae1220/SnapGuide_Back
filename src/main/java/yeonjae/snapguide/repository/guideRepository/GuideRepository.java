package yeonjae.snapguide.repository.guideRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import yeonjae.snapguide.domain.guide.Guide;

import java.util.List;

public interface GuideRepository extends JpaRepository<Guide, Long>, GuideRepositoryCustom {
    List<Guide> findByLocationIdIn(List<Long> locationIds);
}
