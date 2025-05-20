package yeonjae.snapguide.domain.member.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LocalSignInRequestDto {
    private String email;
    private String password;
}
