package yeonjae.snapguide.domain.media;

/**
 * 비동기 파생 파일(썸네일/웹용) 생성 처리 상태
 * - PENDING: 원본 업로드 완료, 비동기 처리 대기 중
 * - COMPLETED: 썸네일/웹용 이미지 생성 완료
 * - FAILED: 비동기 처리 실패 (webKey, thumbnailKey가 null인 채로 유지됨)
 */
public enum ProcessingStatus {
    PENDING,
    COMPLETED,
    FAILED
}
