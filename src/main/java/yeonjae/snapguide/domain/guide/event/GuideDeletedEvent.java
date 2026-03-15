package yeonjae.snapguide.domain.guide.event;

import java.util.List;

/**
 * Guide 삭제 시 발행되는 도메인 이벤트.
 * DB 커밋 이후 S3/로컬 파일 정리를 비동기적으로 처리하기 위해 사용.
 */
public record GuideDeletedEvent(List<MediaFileInfo> mediaFiles) {

    public record MediaFileInfo(String originalKey, String webKey, String thumbnailKey) {}
}
