package yeonjae.snapguide.repository.guideRepository;

/** 썸네일 조회용 경량 프로젝션. guide_id → 첫 번째 media_url 매핑에 사용. */
public interface MediaThumbnailRow {
    Long getGuideId();
    String getMediaUrl();
}
