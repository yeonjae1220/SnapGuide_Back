package yeonjae.snapguide.domain.member.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LocalSignUpRequestDto {
    private String email;
    private String password;
    private String nickname;
}
