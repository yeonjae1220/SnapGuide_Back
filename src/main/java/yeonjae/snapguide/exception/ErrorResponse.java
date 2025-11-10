package yeonjae.snapguide.exception;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 목적
 * 	에러 응답을 JSON 형태로 만들기 위한 객체입니다.
 * 	사용자가 보기 좋은 에러 메시지를 제공하고 디버깅에도 도움을 줍니다.
 *
 * 동작 원리
 * 	ErrorCode에서 가져온 값을 기반으로 각 필드를 채웁니다.
 * 	컨트롤러 또는 예외 처리기에서 이 객체를 ResponseEntity로 감싸 반환합니다.
 */
@Getter
public class ErrorResponse {
    private final LocalDateTime timestamp;
    private final int status;
    private final String error;
    private final String message;

    /**
     * ErrorCode로부터 ErrorResponse 생성
     */
    public ErrorResponse(ErrorCode errorCode) {
        this.timestamp = LocalDateTime.now();
        this.status = errorCode.getStatus().value();
        this.error = errorCode.getStatus().name();
        this.message = errorCode.getMessage();
    }

    /**
     * 커스텀 메시지로 ErrorResponse 생성
     * Spring 기본 예외들의 상세 메시지를 전달할 때 사용
     */
    public ErrorResponse(int status, String error, String message) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
    }
}
