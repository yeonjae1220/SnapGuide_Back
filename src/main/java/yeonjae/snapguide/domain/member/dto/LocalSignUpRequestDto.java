package yeonjae.snapguide.domain.member.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocalSignUpRequestDto {
    private String email;
    private String password;
    private String nickname;
}
