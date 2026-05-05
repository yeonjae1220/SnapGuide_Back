package yeonjae.snapguide.service.mediaSerivce;

import yeonjae.snapguide.domain.media.Media;

import java.util.List;

/**
 * 배치 업로드 결과 — 저장 성공 미디어 목록과 스킵된 파일명 목록을 함께 반환
 *
 * @param savedMedia       저장에 성공한 Media 엔티티 목록
 * @param skippedFileNames 예외 발생으로 스킵된 원본 파일명 목록 (성공 시 빈 리스트)
 */
public record BatchUploadResult(
        List<Media> savedMedia,
        List<String> skippedFileNames
) {
    /** 모든 파일이 성공적으로 저장되었는지 여부 */
    public boolean hasSkipped() {
        return !skippedFileNames.isEmpty();
    }
}
