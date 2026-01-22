package yeonjae.snapguide.repository.guideRepository;

import org.springframework.data.domain.Slice;
import yeonjae.snapguide.controller.guideController.guideDto.GuideResponseDto;

import java.util.List;

public interface GuideRepositoryCustom {
    public List<GuideResponseDto> findAllByMemberId(Long memberId);

    /**
     * 커서 기반 페이징으로 주변 가이드 조회 (기존 방식 - 2단계 쿼리)
     * @param locationIds 검색 범위 내 위치 ID 목록
     * @param lastGuideId 마지막으로 조회한 가이드 ID (커서)
     * @param size 페이지 크기
     * @param currentMemberId 현재 사용자 ID (좋아요 정보 조회용, null 가능)
     * @return 페이징된 가이드 목록 (Slice)
     */
    Slice<GuideResponseDto> findNearbyGuidesPaged(
        List<Long> locationIds,
        Long lastGuideId,
        int size,
        Long currentMemberId
    );

    /**
     * 🚀 최적화된 주변 가이드 조회 (공간 쿼리 + 가이드 조회 통합)
     * - PostGIS 공간 함수와 가이드 조회를 단일 쿼리로 처리
     * - IN 절 제거로 고부하 환경에서 성능 대폭 개선
     *
     * @param lat 위도
     * @param lng 경도
     * @param radiusInDegrees 반경 (degree 단위)
     * @param minLat Bounding Box 최소 위도
     * @param minLng Bounding Box 최소 경도
     * @param maxLat Bounding Box 최대 위도
     * @param maxLng Bounding Box 최대 경도
     * @param lastGuideId 커서 (마지막 가이드 ID)
     * @param size 페이지 크기
     * @param currentMemberId 현재 사용자 ID (좋아요 정보 조회용, null 가능)
     * @return 페이징된 가이드 목록 (Slice)
     */
    Slice<GuideResponseDto> findNearbyGuidesPagedOptimized(
        double lat,
        double lng,
        double radiusInDegrees,
        double minLat,
        double minLng,
        double maxLat,
        double maxLng,
        Long lastGuideId,
        int size,
        Long currentMemberId
    );
}
