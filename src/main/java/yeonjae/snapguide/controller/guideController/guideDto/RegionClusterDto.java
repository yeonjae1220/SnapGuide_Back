package yeonjae.snapguide.controller.guideController.guideDto;

/**
 * 지도 축소 시 국가/대륙 단위로 묶인 가이드 사진 클러스터.
 *
 * @param key          그룹 키 (국가 코드 "KR" 또는 대륙 코드 "AS")
 * @param label        표시 이름 ("South Korea", "Asia")
 * @param lat          그룹 중심 위도 (구성 가이드 좌표 평균)
 * @param lng          그룹 중심 경도
 * @param count        그룹 내 공개 가이드 수
 * @param thumbnailUrl 대표 사진 URL (좋아요 최다 가이드의 첫 미디어, 없으면 null)
 */
public record RegionClusterDto(
        String key,
        String label,
        double lat,
        double lng,
        long count,
        String thumbnailUrl
) {}
