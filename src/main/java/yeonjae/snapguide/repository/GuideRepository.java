package yeonjae.snapguide.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yeonjae.snapguide.entity.guide.Guide;

public interface GuideRepository extends JpaRepository<Guide, Long> {
}
