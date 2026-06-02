package yeonjae.snapguide.repository.guideRepository;

/**
 * 국가/대륙 집계용 경량 프로젝션.
 * 미디어를 제외한 가이드+위치 좌표만 조회해 그룹핑·중심 계산에 사용한다.
 */
public interface GuideAggregateRow {
    Long getGuideId();

    String getCountryCode();

    String getCountry();

    Double getLat();

    Double getLng();

    Integer getLikeCount();
}
