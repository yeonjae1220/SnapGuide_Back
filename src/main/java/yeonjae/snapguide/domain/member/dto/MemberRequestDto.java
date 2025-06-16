package yeonjae.snapguide.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
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
    @NotBlank
    private String email;
    @NotBlank
    private String password;
    private String nickname;
    private List<Authority> authority = new ArrayList<>();

    public Member toEntity(PasswordEncoder passwordEncoder) {
        return Member.builder()
                .email(this.email)
                .password(passwordEncoder.encode(this.password))
                .nickname(this.nickname)
                // .authority(new ArrayList<>(this.authority)) // 복사해서 전달 -> 이유?
                .authority(List.of(Authority.MEMBER)) // 복사해서 전달 -> 이유?
                .build();
    }
}
