package yeonjae.snapguide.repository.guideRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import yeonjae.snapguide.domain.guide.Guide;

public interface GuideRepository extends JpaRepository<Guide, Long>, GuideRepositoryCustom {
}
