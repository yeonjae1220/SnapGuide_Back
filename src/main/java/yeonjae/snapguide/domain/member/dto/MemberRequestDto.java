package yeonjae.snapguide.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import yeonjae.snapguide.domain.member.Authority;
import yeonjae.snapguide.domain.member.Member;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MemberRequestDto {

    /**
     * 회원가입에서만 적용할 비밀번호 복잡도 검증 그룹.
     * 로그인은 {@code @Valid}(Default 그룹)만 거쳐 복잡도 패턴을 강제하지 않는다 —
     * 특수문자 정책 도입 전 가입한 기존 회원이 로그인 시 400으로 잠기는 회귀를 방지.
     */
    public interface SignupValidation {}

    @NotBlank
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하이어야 합니다.")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_#^()\\-]).*$",
            message = "비밀번호는 대문자, 소문자, 숫자, 특수문자(@$!%*?&_#^()-)를 각각 1자 이상 포함해야 합니다.",
            groups = SignupValidation.class
    )
    private String password;

    private String nickname;
    private List<Authority> authority = new ArrayList<>();

    public Member toEntity(PasswordEncoder passwordEncoder) {
        return Member.builder()
                .email(this.email)
                .password(passwordEncoder.encode(this.password))
                .nickname(this.nickname)
                .authority(List.of(Authority.MEMBER))
                .build();
    }
}
