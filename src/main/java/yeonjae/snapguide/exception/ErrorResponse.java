package yeonjae.snapguide.exception;

import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class ErrorResponse {
    private final LocalDateTime timestamp = LocalDateTime.now();
    private final int status;
    private final String error;
    private final String message;

    public ErrorResponse(ErrorCode errorCode) {
        this.status = errorCode.getStatus().value();
        this.error = errorCode.getStatus().name();
        this.message = errorCode.getMessage();
    }
}
