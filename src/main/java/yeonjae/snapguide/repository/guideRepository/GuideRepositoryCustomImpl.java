package yeonjae.snapguide.repository.guideRepository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import yeonjae.snapguide.controller.guideController.guideDto.GuideResponseDto;
import yeonjae.snapguide.domain.guide.Guide;
import yeonjae.snapguide.domain.guide.GuideMapper;
import yeonjae.snapguide.domain.guide.QGuide;
import yeonjae.snapguide.domain.like.QGuideLike;
import yeonjae.snapguide.domain.location.QLocation;
import yeonjae.snapguide.domain.media.QMedia;
import yeonjae.snapguide.domain.mediaMetaData.QMediaMetaData;
import yeonjae.snapguide.domain.cameraModel.QCameraModel;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        QGuideLike gl = QGuideLike.guideLike;
        QMediaMetaData md = QMediaMetaData.mediaMetaData;
        QCameraModel cm = QCameraModel.cameraModel;

        List<Guide> guides = queryFactory
                .selectDistinct(g)
                .from(g)
                .leftJoin(g.author).fetchJoin()
                .leftJoin(g.location, l).fetchJoin()
                .leftJoin(g.mediaList, m).fetchJoin()
                .leftJoin(m.mediaMetaData, md).fetchJoin()
                .leftJoin(md.cameraModel, cm).fetchJoin()
                .where(g.author.id.eq(memberId))
                .fetch();

        // 조회된 가이드가 없으면 빈 리스트를 즉시 반환
        if (guides.isEmpty()) {
            log.info("[GuideRepositoryCustomImpl] No guides found for authorId: {}", memberId);
            return List.of();
        }

        // [변경 2] DTO 매핑을 위해 현재 사용자가 좋아요를 눌렀는지 여부를 별도로 조회합니다.
        // (한 번의 쿼리로 Set에 담아 메모리에서 확인하는 것이 효율적입니다)
        // NOTE: memberId == 로그인한 사용자 ID (내 가이드 조회이므로 작성자 = 현재 사용자)
        // 타인 프로필 조회를 지원하게 되면 currentUserId를 별도 파라미터로 분리해야 함
        Set<Long> likedGuideIds = queryFactory
                .select(gl.guide.id)
                .from(gl)
                .where(gl.member.id.eq(memberId))
                .fetch()
                .stream().collect(Collectors.toSet());

        return guides.stream()
                .map(guide -> GuideMapper.toResponseDtoWithAuthorEmail(guide, likedGuideIds.contains(guide.getId())))
                .toList();
    }



}
