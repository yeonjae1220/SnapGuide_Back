package yeonjae.snapguide.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class MediaRepositoryCustomImpl implements MediaRepositoryCustom{

    // 나중에 querydsl 용
//    private final EntityManager em;
//    private final JPAQueryFactory jpaQueryFactory;
}
