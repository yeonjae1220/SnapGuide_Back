package yeonjae.snapguide.security.authentication.jwt;

import lombok.Data;

@Data
public class TokenRequestDto {
    private String accessToken;
    private String refreshToken;
}
