package yeonjae.snapguide.exception;

import lombok.Getter;

/**
 * 우리가 정의한 ErrorCode를 포함한 예외를 던질 수 있는 수단
 */
@Getter
public class CustomException extends RuntimeException{

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
