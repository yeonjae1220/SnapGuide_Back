package yeonjae.snapguide.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GlobalExceptionHandler 테스트
 * 모든 ErrorCode에 대해 올바른 HTTP 상태 코드와 에러 메시지가 반환되는지 검증
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @DisplayName("CustomException이 발생하면 해당 ErrorCode의 상태와 메시지를 반환한다")
    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void handleCustomException_WithAllErrorCodes_ReturnsCorrectResponse(ErrorCode errorCode) {
        // given
        CustomException exception = new CustomException(errorCode);

        // when
        ResponseEntity<?> response = exceptionHandler.handleCustomException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(errorCode.getStatus());
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);

        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getStatus()).isEqualTo(errorCode.getStatus().value());
        assertThat(errorResponse.getError()).isEqualTo(errorCode.getStatus().name());
        assertThat(errorResponse.getMessage()).isEqualTo(errorCode.getMessage());
    }

    @DisplayName("CustomException - INVALID_INPUT_VALUE 테스트")
    @Test
    void handleCustomException_InvalidInputValue() {
        // given
        CustomException exception = new CustomException(ErrorCode.INVALID_INPUT_VALUE);

        // when
        ResponseEntity<?> response = exceptionHandler.handleCustomException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getMessage()).isEqualTo("잘못된 입력입니다.");
    }

    @DisplayName("CustomException - USER_NOT_FOUND 테스트")
    @Test
    void handleCustomException_UserNotFound() {
        // given
        CustomException exception = new CustomException(ErrorCode.USER_NOT_FOUND);

        // when
        ResponseEntity<?> response = exceptionHandler.handleCustomException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getMessage()).isEqualTo("사용자를 찾을 수 없습니다.");
    }

    @DisplayName("CustomException - INVALID_TOKEN 테스트")
    @Test
    void handleCustomException_InvalidToken() {
        // given
        CustomException exception = new CustomException(ErrorCode.INVALID_TOKEN);

        // when
        ResponseEntity<?> response = exceptionHandler.handleCustomException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getMessage()).isEqualTo("유효하지 않은 토큰입니다.");
    }

    @DisplayName("CustomException - ACCESS_DENIED 테스트")
    @Test
    void handleCustomException_AccessDenied() {
        // given
        CustomException exception = new CustomException(ErrorCode.ACCESS_DENIED);

        // when
        ResponseEntity<?> response = exceptionHandler.handleCustomException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getMessage()).isEqualTo("접근이 거부되었습니다.");
    }

    @DisplayName("CustomException - EXPIRED_TOKEN 테스트")
    @Test
    void handleCustomException_ExpiredToken() {
        // given
        CustomException exception = new CustomException(ErrorCode.EXPIRED_TOKEN);

        // when
        ResponseEntity<?> response = exceptionHandler.handleCustomException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getMessage()).isEqualTo("JWT 토큰이 만료되었습니다.");
    }

    @DisplayName("CustomException - DUPLICATE_USER 테스트")
    @Test
    void handleCustomException_DuplicateUser() {
        // given
        CustomException exception = new CustomException(ErrorCode.DUPLICATE_USER);

        // when
        ResponseEntity<?> response = exceptionHandler.handleCustomException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getMessage()).isEqualTo("이미 가입된 이메일입니다.");
    }

    @DisplayName("CustomException - OAUTH_LOGIN_FAILED 테스트")
    @Test
    void handleCustomException_OAuthLoginFailed() {
        // given
        CustomException exception = new CustomException(ErrorCode.OAUTH_LOGIN_FAILED);

        // when
        ResponseEntity<?> response = exceptionHandler.handleCustomException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getMessage()).isEqualTo("OAuth 로그인에 실패했습니다.");
    }

    @DisplayName("일반 Exception이 발생하면 INTERNAL_SERVER_ERROR를 반환한다")
    @Test
    void handleException_ReturnsInternalServerError() {
        // given
        Exception exception = new RuntimeException("Unexpected error");

        // when
        ResponseEntity<?> response = exceptionHandler.handleException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);

        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(errorResponse.getError()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.name());
        assertThat(errorResponse.getMessage()).isEqualTo("서버 오류입니다.");
    }

    @DisplayName("NullPointerException 발생 시 INTERNAL_SERVER_ERROR를 반환한다")
    @Test
    void handleException_WithNullPointerException_ReturnsInternalServerError() {
        // given
        Exception exception = new NullPointerException("Null value encountered");

        // when
        ResponseEntity<?> response = exceptionHandler.handleException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getMessage()).isEqualTo("서버 오류입니다.");
    }

    @DisplayName("IllegalArgumentException 발생 시 INTERNAL_SERVER_ERROR를 반환한다")
    @Test
    void handleException_WithIllegalArgumentException_ReturnsInternalServerError() {
        // given
        Exception exception = new IllegalArgumentException("Invalid argument");

        // when
        ResponseEntity<?> response = exceptionHandler.handleException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getMessage()).isEqualTo("서버 오류입니다.");
    }

    @DisplayName("ErrorResponse가 timestamp를 포함하는지 확인")
    @Test
    void errorResponse_ContainsTimestamp() {
        // given
        CustomException exception = new CustomException(ErrorCode.USER_NOT_FOUND);

        // when
        ResponseEntity<?> response = exceptionHandler.handleCustomException(exception);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();

        // then
        assertThat(errorResponse.getTimestamp()).isNotNull();
    }

    @DisplayName("MethodArgumentNotValidException - @Valid 검증 실패")
    @Test
    void handleMethodArgumentNotValidException() {
        // given
        org.springframework.validation.BindingResult bindingResult =
                new org.springframework.validation.BeanPropertyBindingResult(new Object(), "memberRequest");
        bindingResult.addError(new org.springframework.validation.FieldError(
                "memberRequest", "email", "", false, null, null, "must not be blank"
        ));
        bindingResult.addError(new org.springframework.validation.FieldError(
                "memberRequest", "password", "", false, null, null, "must not be blank"
        ));

        // Mock the exception to avoid complex internal initialization
        org.springframework.web.bind.MethodArgumentNotValidException exception =
                org.mockito.Mockito.mock(org.springframework.web.bind.MethodArgumentNotValidException.class);
        org.mockito.Mockito.when(exception.getBindingResult()).thenReturn(bindingResult);

        // when
        ResponseEntity<?> response = exceptionHandler.handleMethodArgumentNotValidException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(errorResponse.getMessage()).contains("email", "password");
    }

    @DisplayName("BindException - 바인딩 오류 처리")
    @Test
    void handleBindException() {
        // given
        org.springframework.validation.BindException exception =
                new org.springframework.validation.BindException(new Object(), "object");
        exception.addError(new org.springframework.validation.FieldError(
                "object", "field", "invalid value"
        ));

        // when
        ResponseEntity<?> response = exceptionHandler.handleBindException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(errorResponse.getMessage()).contains("invalid value");
    }

    @DisplayName("ConstraintViolationException - Bean Validation 제약 조건 위반")
    @Test
    void handleConstraintViolationException() {
        // given
        java.util.Set<jakarta.validation.ConstraintViolation<?>> violations = new java.util.HashSet<>();
        jakarta.validation.ConstraintViolationException exception =
                new jakarta.validation.ConstraintViolationException("Validation failed", violations);

        // when
        ResponseEntity<?> response = exceptionHandler.handleConstraintViolationException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("HttpMessageNotReadableException - JSON 파싱 오류")
    @Test
    void handleHttpMessageNotReadableException() {
        // given
        org.springframework.http.converter.HttpMessageNotReadableException exception =
                new org.springframework.http.converter.HttpMessageNotReadableException(
                        "JSON parse error", (org.springframework.http.HttpInputMessage) null
                );

        // when
        ResponseEntity<?> response = exceptionHandler.handleHttpMessageNotReadableException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(errorResponse.getMessage()).contains("JSON");
    }

    @DisplayName("HttpRequestMethodNotSupportedException - 지원하지 않는 HTTP 메소드")
    @Test
    void handleHttpRequestMethodNotSupportedException() {
        // given
        org.springframework.web.HttpRequestMethodNotSupportedException exception =
                new org.springframework.web.HttpRequestMethodNotSupportedException("POST");

        // when
        ResponseEntity<?> response = exceptionHandler.handleHttpRequestMethodNotSupportedException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
        assertThat(errorResponse.getMessage()).contains("POST");
    }

    @DisplayName("MissingServletRequestParameterException - 필수 파라미터 누락")
    @Test
    void handleMissingServletRequestParameterException() {
        // given
        org.springframework.web.bind.MissingServletRequestParameterException exception =
                new org.springframework.web.bind.MissingServletRequestParameterException("userId", "String");

        // when
        ResponseEntity<?> response = exceptionHandler.handleMissingServletRequestParameterException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(errorResponse.getMessage()).contains("userId");
    }

    @DisplayName("AccessDeniedException - Spring Security 접근 거부")
    @Test
    void handleAccessDeniedException() {
        // given
        org.springframework.security.access.AccessDeniedException exception =
                new org.springframework.security.access.AccessDeniedException("Access is denied");

        // when
        ResponseEntity<?> response = exceptionHandler.handleAccessDeniedException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(errorResponse.getMessage()).isEqualTo("접근이 거부되었습니다.");
    }

    @DisplayName("ErrorResponse - 커스텀 생성자 테스트")
    @Test
    void errorResponse_CustomConstructor() {
        // given
        int status = 400;
        String error = "BAD_REQUEST";
        String message = "Custom error message";

        // when
        ErrorResponse errorResponse = new ErrorResponse(status, error, message);

        // then
        assertThat(errorResponse.getStatus()).isEqualTo(status);
        assertThat(errorResponse.getError()).isEqualTo(error);
        assertThat(errorResponse.getMessage()).isEqualTo(message);
        assertThat(errorResponse.getTimestamp()).isNotNull();
    }
}
