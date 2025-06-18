package yeonjae.snapguide.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 목적
 * 	프로젝트 전역에서 발생하는 예외를 중앙에서 관리합니다.
 * 	예외마다 일관된 JSON 구조로 응답할 수 있게 합니다.
 *
 * 동작 원리
 * 	@RestControllerAdvice는 전역 예외 처리 역할을 합니다.
 * 	@ExceptionHandler(CustomException.class)는 CustomException이 발생하면 실행됩니다.
 * 	내부에서 new ErrorResponse(errorCode)를 만들어 JSON 형태로 응답을 반환합니다.
 *
 * 	1.	예외가 던져지면 GlobalExceptionHandler가 감지합니다.
 * 	2.	ErrorCode.INVALID_TOKEN에 설정된 status/message가 그대로 클라이언트에 JSON 응답으로 전달됩니다.
 *
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<?> handleCustomException(CustomException ex) {
        return ResponseEntity
                .status(ex.getErrorCode().getStatus())
                .body(new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception ex) {
        ex.printStackTrace(); // 로그 출력
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
