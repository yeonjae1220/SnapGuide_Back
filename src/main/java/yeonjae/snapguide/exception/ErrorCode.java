package yeonjae.snapguide.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 예외 발생 시 반환할 HTTP 상태 코드, 에러 메시지, 에러 분류를 미리 정의해 놓은 enum
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 공통 오류
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류입니다."),

    // 인증 관련
    DUPLICATE_USER(HttpStatus.BAD_REQUEST, "이미 가입된 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "Token을 찾을 수 없습니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "JWT 토큰이 만료되었습니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Refresh Token이 만료되었습니다."),
    INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED, "잘못된 JWT 서명입니다."),
    MALFORMED_HEADER(HttpStatus.BAD_REQUEST, "요청 헤더 형식이 잘못되었습니다."),
    MISSING_AUTH_HEADER(HttpStatus.UNAUTHORIZED, "Authorization 헤더가 존재하지 않습니다."),
    OAUTH_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "OAuth 로그인에 실패했습니다."),

    // 기타
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근이 거부되었습니다.");

    private final HttpStatus status;
    private final String message;

}
