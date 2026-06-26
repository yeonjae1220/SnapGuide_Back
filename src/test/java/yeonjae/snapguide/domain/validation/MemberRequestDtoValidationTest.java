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
import yeonjae.snapguide.domain.member.dto.MemberRequestDto;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MemberRequestDto 유효성 검증")
class MemberRequestDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private MemberRequestDto validRequest() {
        return MemberRequestDto.builder()
                .email("valid@example.com")
                .password("ValidPass1!")
                .nickname("user")
                .build();
    }

    @Nested
    @DisplayName("이메일 검증")
    class EmailValidation {

        @ParameterizedTest
        @DisplayName("올바른 이메일 형식은 검증을 통과한다")
        @ValueSource(strings = {"user@example.com", "test.name+tag@domain.co.kr", "admin@company.org"})
        void shouldPassForValidEmails(String email) {
            // Arrange
            MemberRequestDto request = MemberRequestDto.builder()
                    .email(email)
                    .password("ValidPass1!")
                    .nickname("user")
                    .build();

            // Act
            Set<ConstraintViolation<MemberRequestDto>> violations = validator.validate(request);

            // Assert
            assertThat(violations).isEmpty();
        }

        @ParameterizedTest
        @DisplayName("잘못된 이메일 형식은 검증에 실패한다")
        @ValueSource(strings = {"notanemail", "missing@", "@nodomain.com", "spaces in@email.com"})
        void shouldFailForInvalidEmails(String email) {
            // Arrange
            MemberRequestDto request = MemberRequestDto.builder()
                    .email(email)
                    .password("ValidPass1!")
                    .nickname("user")
                    .build();

            // Act
            Set<ConstraintViolation<MemberRequestDto>> violations = validator.validate(request);

            // Assert
            assertThat(violations)
                    .isNotEmpty()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }

        @Test
        @DisplayName("이메일이 빈 문자열이면 검증에 실패한다")
        void shouldFailForBlankEmail() {
            // Arrange
            MemberRequestDto request = MemberRequestDto.builder()
                    .email("")
                    .password("ValidPass1")
                    .nickname("user")
                    .build();

            // Act
            Set<ConstraintViolation<MemberRequestDto>> violations = validator.validate(request);

            // Assert
            assertThat(violations)
                    .isNotEmpty()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }
    }

    @Nested
    @DisplayName("비밀번호 검증")
    class PasswordValidation {

        @ParameterizedTest
        @DisplayName("대소문자, 숫자, 특수문자를 포함하고 8자 이상인 비밀번호는 검증을 통과한다")
        @ValueSource(strings = {"ValidPass1!", "MySecure9Password$", "Abcdefg1#"})
        void shouldPassForValidPasswords(String password) {
            // Arrange
            MemberRequestDto request = MemberRequestDto.builder()
                    .email("valid@example.com")
                    .password(password)
                    .nickname("user")
                    .build();

            // Act — signup 검증 그룹으로 복잡도 패턴까지 적용
            Set<ConstraintViolation<MemberRequestDto>> violations =
                    validator.validate(request, MemberRequestDto.SignupValidation.class);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("7자 비밀번호는 최소 길이 제약으로 실패한다")
        void shouldFailForTooShortPassword() {
            // Arrange
            MemberRequestDto request = MemberRequestDto.builder()
                    .email("valid@example.com")
                    .password("Short1")
                    .nickname("user")
                    .build();

            // Act
            Set<ConstraintViolation<MemberRequestDto>> violations = validator.validate(request);

            // Assert
            assertThat(violations)
                    .isNotEmpty()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }

        @Test
        @DisplayName("소문자만 있는 비밀번호는 패턴 제약으로 실패한다")
        void shouldFailForPasswordWithoutUppercase() {
            // Arrange
            MemberRequestDto request = MemberRequestDto.builder()
                    .email("valid@example.com")
                    .password("lowercase1")
                    .nickname("user")
                    .build();

            // Act
            Set<ConstraintViolation<MemberRequestDto>> violations =
                    validator.validate(request, MemberRequestDto.SignupValidation.class);

            // Assert
            assertThat(violations)
                    .isNotEmpty()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }

        @Test
        @DisplayName("숫자 없는 비밀번호는 패턴 제약으로 실패한다")
        void shouldFailForPasswordWithoutDigit() {
            // Arrange
            MemberRequestDto request = MemberRequestDto.builder()
                    .email("valid@example.com")
                    .password("NoDigitPass")
                    .nickname("user")
                    .build();

            // Act
            Set<ConstraintViolation<MemberRequestDto>> violations =
                    validator.validate(request, MemberRequestDto.SignupValidation.class);

            // Assert
            assertThat(violations)
                    .isNotEmpty()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }

        @Test
        @DisplayName("대문자만 있고 숫자가 없는 비밀번호는 패턴 제약으로 실패한다")
        void shouldFailForPasswordWithoutLowercase() {
            // Arrange
            MemberRequestDto request = MemberRequestDto.builder()
                    .email("valid@example.com")
                    .password("ALLUPPERCASE1")
                    .nickname("user")
                    .build();

            // Act
            Set<ConstraintViolation<MemberRequestDto>> violations =
                    validator.validate(request, MemberRequestDto.SignupValidation.class);

            // Assert
            assertThat(violations)
                    .isNotEmpty()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }

        @Test
        @DisplayName("특수문자가 없는 비밀번호는 패턴 제약으로 실패한다")
        void shouldFailForPasswordWithoutSpecialCharacter() {
            // Arrange
            MemberRequestDto request = MemberRequestDto.builder()
                    .email("valid@example.com")
                    .password("NoSpecialChar1")
                    .nickname("user")
                    .build();

            // Act
            Set<ConstraintViolation<MemberRequestDto>> violations =
                    validator.validate(request, MemberRequestDto.SignupValidation.class);

            // Assert
            assertThat(violations)
                    .isNotEmpty()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }
    }
}
