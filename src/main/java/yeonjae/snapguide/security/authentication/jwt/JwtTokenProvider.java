package yeonjae.snapguide.security.authentication.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {
    private final SecretKey key;

    // AccessToken 유효기간 설정 : 30분
    @Value("${jwt.access_token_expiration}")
    private long expAccess;
    // RefreshToken 유효기간 설정 : 1주
    @Value("${jwt.refresh_token_expiration}")
    private long expRefresh;
    // refresh 체크용
    private static final long THREE_DAYS = 1000 * 60 * 60 * 24 * 3;  // 3일

    public static final String AUTHORIZATION_HEADER = "auth"; // "Authorization";
    public static final String BEARER_PREFIX = "Bearer";

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // JWT Access Token 생성
    // TODO : 매개변수 확인, 좀 된 코드에서는 Authentication authentication 으로 받아서 .getAuthorities().stream()으로 가져오네
    public JwtToken generateToken(Collection<? extends GrantedAuthority> authorityInfo,
                                   String id) {
        System.out.println("Authority info: " + authorityInfo); // 디버깅용
        // 사용자의 권한 정보들을 모아 문자열로 만든다.
        String authorities = authorityInfo.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(",")); // "MEMBER,ADMIN"
        log.info("token authorities : " + authorities);
        for (GrantedAuthority authority : authorityInfo) {
            log.info("권한: " + authority.getAuthority());
        }

        long now = (new Date()).getTime();

        // AccessToken 생성 과정
        Date accessTokenExpiresIn = new Date(now + expAccess);
        String accessToken = Jwts.builder()
                .subject(id)
                .issuedAt(new Date(now))
                .expiration(accessTokenExpiresIn)
                .claim(AUTHORIZATION_HEADER, authorities)
                .signWith(key)
//                .signWith(key, SignatureAlgorithm.HS256) // 알고리즘은 키에서 추론하는 것으로 변경됨
                .compact();

        // RefreshToken 생성 과정
        Date refreshTokenExpiresIn = new Date(now + expRefresh);
        String refreshToken = Jwts.builder()
                .issuedAt(new Date(now))
                .expiration(refreshTokenExpiresIn)
                .signWith(key)
                .compact();

        return JwtToken.builder()
                .grantType(BEARER_PREFIX)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // Access Token에 들어있는 정보를 꺼내 Authentication 객체를 생성 후 반환한다.
    public Authentication getAuthentication(String token) {
        // 토큰의 Payload에 저장된 Claim들을 추출한다. (토큰 복호화)
        Claims claims = parseClaims(token);
        log.info("token authorities : " + claims.toString());

        if (claims.get(AUTHORIZATION_HEADER) == null) {
            // 권한 정보 없는 토큰
            // TODO : 별도의 예외 처리 필요
            log.info("권한 정보 없는 토큰");
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
            // throw new InvalidTokenException(INVALID_TOKEN.getMessage());
        }

        // Claim에서 권한 정보를 추출한다.
        // 1. "MEMBER,ADMIN" → [SimpleGrantedAuthority("MEMBER"), ...]
        Collection<? extends GrantedAuthority> authorities = Arrays
                .stream(claims.get(AUTHORIZATION_HEADER).toString().split(","))
                .map(String::trim)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        log.info("복원된 권한들: {}", authorities);

        // Claim에 저장된 사용자 아이디를 통해 UserDetails 객체를 생성해서
        UserDetails principal = new User(claims.getSubject(), "", authorities);
        // Authentication 객체를 생성하여 반환한다.
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    private Claims parseClaims(String token) {
        try {
            // https://github.com/jwtk/jjwt#quickstart 이거 참고해서 넣음.
            // SecretKey key = Jwts.SIG.HS256.key().build();
            return Jwts.parser() // parserBuilder 이거 없어졌나?
                    .verifyWith(key)
                    // .setSigningKey(key)
                    .build()
                    .parseSignedClaims(token)
//                    .parseClaimsJws(token)
                    .getPayload();
                    // .getBody();
        } catch (ExpiredJwtException e) {
            // TODO : 별도의 예외 처리 필요
            // throw new ExpiredTokenException(EXPIRED_TOKEN.getMessage());
            log.info("만료된 토큰");
//            throw new AuthenticationCredentialsNotFoundException("만료된 토큰입니다.");
            return e.getClaims();
        }
    }

    // 토큰 검증 메서드
    public boolean validateToken(String token) { // TODO : 별도 예외 처리 필요 throws TokenException
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            // TODO : 예외 처리 로직?
            log.info("validateToken true 반환");
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.");
        }
        log.info("validateToken false 반환");
        return false;
        }

    public boolean refreshTokenPeriodCheck(String token) {
        Jws<Claims> claimsJws =
                Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token);
        long now = (new Date()).getTime();
        long refresh_expiredTime = claimsJws.getPayload().getExpiration().getTime();
        long refresh_nowTime = new Date(now + expRefresh).getTime();

        if (refresh_nowTime - refresh_expiredTime > THREE_DAYS) {
            return true;
        }
        return false;
    }

    // Http Request의 Header로부터 Access Token을 추출하는 메서드.
    // Authorization Header 를 통해 인증
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        // TODO : 이부분 코드 해석 한번더
        log.debug("bearertoken : {}", bearerToken);
        if (StringUtils.hasText(bearerToken)) {
            if (bearerToken.startsWith(BEARER_PREFIX) && bearerToken.length() > 7) {
                int tokenStartIndex = 7;
                return bearerToken.substring(tokenStartIndex);
            }
            // TODO : 별도 예외 처리 필요
            // throw new MalformedHeaderException(MALFORMED_HEADER.getMessage());
            log.info("토큰 예외 MalformedHeaderException");
        }
        return bearerToken; // TODO : 여기도 예외처리 해줘야할듯
    }

}






