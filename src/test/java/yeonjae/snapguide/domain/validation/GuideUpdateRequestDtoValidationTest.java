package yeonjae.snapguide.domain.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import yeonjae.snapguide.controller.guideController.guideDto.GuideUpdateRequestDto;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GuideUpdateRequestDto 유효성 검증")
class GuideUpdateRequestDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Nested
    @DisplayName("tip 필드 검증")
    class TipValidation {

        @Test
        @DisplayName("유효한 tip 값은 검증을 통과한다")
        void shouldPassForValidTip() {
            // Arrange
            GuideUpdateRequestDto dto = new GuideUpdateRequestDto("This is a valid tip");

            // Act
            Set<ConstraintViolation<GuideUpdateRequestDto>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("tip이 빈 문자열이면 검증에 실패한다")
        void shouldFailForBlankTip() {
            // Arrange
            GuideUpdateRequestDto dto = new GuideUpdateRequestDto("");

            // Act
            Set<ConstraintViolation<GuideUpdateRequestDto>> violations = validator.validate(dto);

            // Assert
            assertThat(violations)
                    .isNotEmpty()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("tip")
                            && v.getMessage().equals("tip은 필수입니다."));
        }

        @Test
        @DisplayName("tip이 공백 문자열이면 검증에 실패한다")
        void shouldFailForWhitespaceTip() {
            // Arrange
            GuideUpdateRequestDto dto = new GuideUpdateRequestDto("   ");

            // Act
            Set<ConstraintViolation<GuideUpdateRequestDto>> violations = validator.validate(dto);

            // Assert
            assertThat(violations)
                    .isNotEmpty()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("tip"));
        }

        @Test
        @DisplayName("tip이 null이면 검증에 실패한다")
        void shouldFailForNullTip() {
            // Arrange
            GuideUpdateRequestDto dto = new GuideUpdateRequestDto(null);

            // Act
            Set<ConstraintViolation<GuideUpdateRequestDto>> violations = validator.validate(dto);

            // Assert
            assertThat(violations)
                    .isNotEmpty()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("tip"));
        }

        @ParameterizedTest
        @DisplayName("다양한 유효한 tip 내용은 검증을 통과한다")
        @ValueSource(strings = {
                "사진 찍기 좋은 장소입니다.",
                "Best photo spot near the lake!",
                "A",
                "Long tip content that spans more characters is perfectly fine as there is no max length constraint"
        })
        void shouldPassForVariousValidTips(String tip) {
            // Arrange
            GuideUpdateRequestDto dto = new GuideUpdateRequestDto(tip);

            // Act
            Set<ConstraintViolation<GuideUpdateRequestDto>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).isEmpty();
        }
    }
}
