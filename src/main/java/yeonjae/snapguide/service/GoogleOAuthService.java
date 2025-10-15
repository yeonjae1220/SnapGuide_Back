package yeonjae.snapguide.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import yeonjae.snapguide.domain.member.Authority;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.exception.CustomException;
import yeonjae.snapguide.exception.ErrorCode;
import yeonjae.snapguide.infrastructure.cache.redis.RedisRefreshToken;
import yeonjae.snapguide.repository.RedisRefreshTokenRepository;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;
import yeonjae.snapguide.security.authentication.jwt.JwtToken;
import yeonjae.snapguide.security.authentication.jwt.JwtTokenProvider;

import java.util.Collections;
import java.util.Map;

/**
 * Google OAuth 모바일 앱 인증 처리
 * - 앱에서 받은 authorization code를 Google API로 토큰 교환
 * - 사용자 정보 조회 및 회원가입/로그인 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisRefreshTokenRepository redisRefreshTokenRepository;

    @Value("${spring.security.oauth2.client.registration.google-ios.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google-ios.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.google-ios.redirect-uri}")
    private String redirectUri;

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    /**
     * Google Authorization Code를 JWT 토큰으로 교환
     * @param authorizationCode 앱에서 받은 Google authorization code
     * @return JWT 토큰 (accessToken, refreshToken)
     */
    @Transactional
    public JwtToken exchangeGoogleAuthorizationCode(String authorizationCode) {
        log.info("Google Authorization Code 교환 시작: {}", authorizationCode.substring(0, 10) + "...");

        // 1. Google에 authorization code로 access token 요청
        String googleAccessToken = getGoogleAccessToken(authorizationCode);

        // 2. Google access token으로 사용자 정보 조회
        Map<String, Object> userInfo = getGoogleUserInfo(googleAccessToken);

        String email = (String) userInfo.get("email");
        String name = (String) userInfo.get("name");

        log.info("Google 사용자 정보 조회 성공: email={}, name={}", email, name);

        // 3. 회원 조회 또는 생성
        Member member = memberRepository.findByEmail(email)
                .orElseGet(() -> createNewMember(email, name));

        // 4. JWT 토큰 생성
        JwtToken jwtToken = jwtTokenProvider.generateToken(member.getAuthority(), member.getEmail());

        // 5. RefreshToken Redis에 저장
        RedisRefreshToken refreshToken = RedisRefreshToken.builder()
                .key(email)
                .value(jwtToken.getRefreshToken())
                .build();
        redisRefreshTokenRepository.save(refreshToken);

        log.info("Google OAuth 로그인 성공 - JWT 토큰 발급 for user: {}", email);

        return jwtToken;
    }

    /**
     * Google에 authorization code를 보내서 access token 받기
     */
    private String getGoogleAccessToken(String authorizationCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", authorizationCode);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(GOOGLE_TOKEN_URL, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String accessToken = (String) response.getBody().get("access_token");
                log.info("Google access token 발급 성공");
                return accessToken;
            } else {
                throw new CustomException(ErrorCode.INVALID_TOKEN);
            }
        } catch (Exception e) {
            log.error("Google access token 발급 실패", e);
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    /**
     * Google access token으로 사용자 정보 조회
     */
    private Map<String, Object> getGoogleUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    GOOGLE_USER_INFO_URL,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new CustomException(ErrorCode.USER_NOT_FOUND);
            }
        } catch (Exception e) {
            log.error("Google 사용자 정보 조회 실패", e);
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
    }

    /**
     * 새로운 Google 사용자 생성
     */
    private Member createNewMember(String email, String name) {
        Member newMember = Member.builder()
                .email(email)
                .password("") // OAuth 사용자는 비밀번호 없음
                .authority(Collections.singletonList(Authority.MEMBER))
                .build();

        Member savedMember = memberRepository.save(newMember);
        log.info("신규 Google 사용자 생성: {}", email);

        return savedMember;
    }
}
