package yeonjae.snapguide.repository.guideRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeonjae.snapguide.domain.guide.Guide;


import java.util.List;
import java.util.Optional;

public interface GuideRepository extends JpaRepository<Guide, Long>, GuideRepositoryCustom {
    List<Guide> findByLocationIdIn(List<Long> locationIds);

    /**
     * Fetch Join을 사용하여 Guide와 연관된 Media, Author, Location을 한 번에 조회
     * N+1 문제 해결
     */
    @Query("""
        SELECT DISTINCT g FROM Guide g
        LEFT JOIN FETCH g.mediaList
        LEFT JOIN FETCH g.author
        LEFT JOIN FETCH g.location
        WHERE g.location.id IN :locationIds
        """)
    List<Guide> findByLocationIdInWithFetch(@Param("locationIds") List<Long> locationIds);

    /**
     * Guide 단건 조회 시 연관 엔티티 함께 조회
     */
    @Query("""
        SELECT g FROM Guide g
        LEFT JOIN FETCH g.mediaList
        LEFT JOIN FETCH g.author
        LEFT JOIN FETCH g.location
        WHERE g.id = :id
        """)
    Optional<Guide> findByIdWithFetch(@Param("id") Long id);

}
