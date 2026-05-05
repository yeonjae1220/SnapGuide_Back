package yeonjae.snapguide.controller.guideController.guideDto;

import java.util.List;

/**
 * 가이드 생성 응답 DTO
 * 업로드 성공 여부 및 스킵된 파일 정보를 함께 반환한다.
 *
 * @param guideId      생성된 Guide ID
 * @param totalFiles   요청한 전체 파일 수
 * @param savedFiles   실제 저장 성공한 파일 수
 * @param skippedFiles 스킵된 파일의 원본 파일명 목록 (모두 성공 시 빈 리스트)
 * @param message      결과 메시지
 */
public record GuideCreateResponseDto(
        Long guideId,
        int totalFiles,
        int savedFiles,
        List<String> skippedFiles,
        String message
) {}
