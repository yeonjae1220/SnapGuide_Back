package yeonjae.snapguide.domain.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import yeonjae.snapguide.controller.pushController.PushSubscribeRequest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PushSubscribeRequest 유효성 검증")
class PushSubscribeRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private PushSubscribeRequest validRequest() {
        return new PushSubscribeRequest(
                "https://fcm.googleapis.com/fcm/send/valid-endpoint",
                new PushSubscribeRequest.Keys("authKeyBase64==", "p256dhKeyBase64==")
        );
    }

    @Test
    @DisplayName("모든 필드가 유효하면 검증을 통과한다")
    void shouldPassForValidRequest() {
        // Act
        Set<ConstraintViolation<PushSubscribeRequest>> violations = validator.validate(validRequest());

        // Assert
        assertThat(violations).isEmpty();
    }

    @Nested
    @DisplayName("endpoint 필드 검증")
    class EndpointValidation {

        @Test
        @DisplayName("endpoint가 빈 문자열이면 검증에 실패한다")
        void shouldFailForBlankEndpoint() {
            // Arrange
            PushSubscribeRequest request = new PushSubscribeRequest(
                    "",
                    new PushSubscribeRequest.Keys("authKey", "p256dhKey")
            );

            // Act
            Set<ConstraintViolation<PushSubscribeRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations)
                    .isNotEmpty()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("endpoint"));
        }

        @Test
        @DisplayName("endpoint가 null이면 검증에 실패한다")
        void shouldFailForNullEndpoint() {
            // Arrange
            PushSubscribeRequest request = new PushSubscribeRequest(
                    null,
                    new PushSubscribeRequest.Keys("authKey", "p256dhKey")
            );

            // Act
            Set<ConstraintViolation<PushSubscribeRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations)
                    .isNotEmpty()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("endpoint"));
        }
    }

    @Nested
    @DisplayName("Keys 내부 레코드 검증")
    class KeysValidation {

        @Test
        @DisplayName("auth 키가 빈 문자열이면 검증에 실패한다")
        void shouldFailForBlankAuth() {
            // Arrange
            PushSubscribeRequest request = new PushSubscribeRequest(
                    "https://valid-endpoint.com",
                    new PushSubscribeRequest.Keys("", "p256dhKey")
            );

            // Act
            Set<ConstraintViolation<PushSubscribeRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations)
                    .isNotEmpty()
                    .anyMatch(v -> v.getPropertyPath().toString().contains("auth"));
        }

        @Test
        @DisplayName("p256dh 키가 빈 문자열이면 검증에 실패한다")
        void shouldFailForBlankP256dh() {
            // Arrange
            PushSubscribeRequest request = new PushSubscribeRequest(
                    "https://valid-endpoint.com",
                    new PushSubscribeRequest.Keys("authKey", "")
            );

            // Act
            Set<ConstraintViolation<PushSubscribeRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations)
                    .isNotEmpty()
                    .anyMatch(v -> v.getPropertyPath().toString().contains("p256dh"));
        }
    }
}
