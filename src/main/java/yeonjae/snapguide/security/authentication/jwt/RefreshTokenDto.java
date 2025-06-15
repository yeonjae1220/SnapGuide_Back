package yeonjae.snapguide.security.authentication.jwt;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RefreshTokenDto {
    @NotEmpty
    String refreshToken;
}
