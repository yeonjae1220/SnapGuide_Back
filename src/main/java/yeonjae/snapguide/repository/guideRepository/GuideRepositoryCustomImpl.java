package yeonjae.snapguide.repository.guideRepository;

import com.querydsl.core.Tuple;
//import com.querydsl.core.group.GroupBy;
import com.querydsl.core.group.GroupBy;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import yeonjae.snapguide.controller.guideController.guideDto.GuideResponseDto;
import yeonjae.snapguide.domain.guide.QGuide;
import yeonjae.snapguide.domain.location.QLocation;
import yeonjae.snapguide.domain.media.MediaDto;
import yeonjae.snapguide.domain.media.QMedia;

import java.util.List;
import java.util.Map;
@Slf4j
@RequiredArgsConstructor
@Repository
public class GuideRepositoryCustomImpl implements GuideRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public List<GuideResponseDto> findAllByMemberId(Long memberId) {
        QGuide g = QGuide.guide;
        QMedia m = QMedia.media;
        QLocation l = QLocation.location;

        // ① 가이드 + 위치 한 번에 fetch
        List<Tuple> guides = queryFactory
                .select(g.id, g.tip, l.locationName)
                .from(g)
                .leftJoin(g.location, l) // HACK : fetchJoin?
                .where(g.author.id.eq(memberId))
                .fetch();

        // ② 가이드 id 모아 1쿼리로 미디어 10장까지 조회 (N + 1 방지)
        List<Long> guideIds = guides.stream()
                .map(t -> t.get(g.id))
                .toList();

        // 예외처리 guideIds가 비어있을경우
        if (guides.isEmpty()) {
            log.info("[GuideRepositoryCustomImpl] guideIds size : " + guideIds.size());
            return List.of(); // 프론트에 빈 리스트 응답
        }

        // id IN (...) 쿼리로 “가이드 id –> 미디어 10장까지” 한 번에 가져오기
        Map<Long, List<MediaDto>> mediaMap = queryFactory
                .select(m.guide.id, m.mediaUrl) // (key, value) 컬럼 지정
                .from(m)
                .where(m.guide.id.in(guideIds))
                .orderBy(m.id.asc())                // id 순으로 10장 잘라오기용 NOTE : ID 순 정렬이라 추후 동작 확인 필요
                // 결과를 그룹화 변환하기
                .transform(GroupBy.groupBy(m.guide.id).as(
                        GroupBy.list(Projections.constructor(MediaDto.class, m.mediaName, m.mediaUrl))
                ));

        // ③ DTO 매핑
        return guides.stream()
                .map(t -> new GuideResponseDto(
                        t.get(g.id),
                        t.get(g.tip),
                        t.get(l.locationName),
                        mediaMap.getOrDefault(t.get(g.id), List.of())
                )).toList();

    }

}
