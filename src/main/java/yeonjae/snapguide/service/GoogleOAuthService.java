package yeonjae.snapguide.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.exception.CustomException;
import yeonjae.snapguide.exception.ErrorCode;
import yeonjae.snapguide.infrastructure.cache.redis.RedisRefreshToken;
import yeonjae.snapguide.repository.RedisRefreshTokenRepository;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;
import yeonjae.snapguide.security.authentication.jwt.JwtToken;
import yeonjae.snapguide.security.authentication.jwt.JwtTokenProvider;
import org.springframework.transaction.annotation.Transactional;

/**
 * Google OAuth 직접 통신 서비스 (Option 1: 앱 중심 OAuth)
 * - 모바일 앱이 Google로부터 직접 받은 authorization code를 처리
 * - Google API와 직접 통신하여 access token 획득 및 사용자 정보 조회
 * - JWT 토큰 생성 및 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisRefreshTokenRepository redisRefreshTokenRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Web Client ID와 Secret (백엔드에서 Google API 호출용)
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleWebClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleWebClientSecret;

    // iOS Client ID (앱에서 사용)
    private static final String GOOGLE_IOS_CLIENT_ID = "1054453650839-jae2hnasojd3b6lkrl2ki3au2vve6hut.apps.googleusercontent.com";

    // Google OAuth endpoints
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    /**
     * Google authorization code를 JWT 토큰으로 교환
     * @param code 앱이 Google로부터 받은 authorization code
     * @return JwtToken (accessToken, refreshToken)
     */
    @Transactional
    public JwtToken exchangeCodeForToken(String code) {
        try {
            // 1. Google에 authorization code를 보내서 access token 획득
            String googleAccessToken = getGoogleAccessToken(code);

            // 2. Google access token으로 사용자 정보 조회
            GoogleUserInfo userInfo = getGoogleUserInfo(googleAccessToken);

            // 3. 회원 찾기 또는 생성
            Member member = findOrCreateMember(userInfo);

            // 4. JWT 토큰 생성
            JwtToken jwtToken = jwtTokenProvider.generateToken(member.getAuthority(), member.getEmail());

            // 5. RefreshToken Redis에 저장
            RedisRefreshToken refreshToken = RedisRefreshToken.builder()
                    .key(member.getEmail())
                    .value(jwtToken.getRefreshToken())
                    .build();
            redisRefreshTokenRepository.save(refreshToken);

            log.info("✅ Google OAuth 로그인 성공 for user: {}", member.getEmail());

            return jwtToken;

        } catch (Exception e) {
            log.error("⛔️ Google OAuth 로그인 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.OAUTH_LOGIN_FAILED);
        }
    }

    /**
     * Google authorization code를 Google access token으로 교환
     */
    private String getGoogleAccessToken(String code) {
        try {
            // 요청 파라미터 구성
            // Web Client ID와 Secret 사용 (앱에서 받은 code와 일치해야 함)
            String redirectUri = "com.yeonjae.snapguide://oauth/callback";

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("code", code);
            params.add("client_id", googleWebClientId);  // Web Client ID
            params.add("client_secret", googleWebClientSecret);  // Web Client Secret
            params.add("redirect_uri", redirectUri);
            params.add("grant_type", "authorization_code");

            log.info("🔐 Google token 교환 요청 - client_id: {}, redirect_uri: {}", googleWebClientId, redirectUri);

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            log.info("🔐 Google에 access token 요청 중...");

            // Google에 POST 요청
            ResponseEntity<String> response = restTemplate.exchange(
                    GOOGLE_TOKEN_URL,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Google token 요청 실패: " + response.getStatusCode());
            }

            // JSON 파싱하여 access_token 추출
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            String accessToken = jsonNode.get("access_token").asText();

            log.info("✅ Google access token 획득 성공");

            return accessToken;

        } catch (Exception e) {
            log.error("⛔️ Google access token 획득 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Google access token 획득 실패", e);
        }
    }

    /**
     * Google access token으로 사용자 정보 조회
     */
    private GoogleUserInfo getGoogleUserInfo(String accessToken) {
        try {
            // HTTP 헤더에 access token 추가
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.info("👤 Google 사용자 정보 요청 중...");

            // Google UserInfo API 호출
            ResponseEntity<String> response = restTemplate.exchange(
                    GOOGLE_USERINFO_URL,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Google 사용자 정보 조회 실패: " + response.getStatusCode());
            }

            // JSON 파싱
            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            GoogleUserInfo userInfo = GoogleUserInfo.builder()
                    .email(jsonNode.get("email").asText())
                    .name(jsonNode.has("name") ? jsonNode.get("name").asText() : null)
                    .picture(jsonNode.has("picture") ? jsonNode.get("picture").asText() : null)
                    .build();

            log.info("✅ Google 사용자 정보 획득 성공: {}", userInfo.getEmail());

            return userInfo;

        } catch (Exception e) {
            log.error("⛔️ Google 사용자 정보 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Google 사용자 정보 조회 실패", e);
        }
    }

    /**
     * Google 사용자 정보로 회원 찾기 또는 생성
     */
    private Member findOrCreateMember(GoogleUserInfo userInfo) {
        // ✅ authority를 함께 조회하여 N+1 방지 (JWT 생성 시 필요)
        return memberRepository.findByEmailWithAuthority(userInfo.getEmail())
                .orElseGet(() -> {
                    log.info("🆕 새로운 Google 회원 생성: {}", userInfo.getEmail());
                    Member newMember = Member.builder()
                            .email(userInfo.getEmail())
                            .password("") // OAuth 회원은 비밀번호 없음
                            .build();
                    // 신규 회원은 authority가 자동 설정되므로 바로 반환
                    return memberRepository.save(newMember);
                });
    }

    /**
     * Google 사용자 정보 DTO
     */
    @lombok.Builder
    @lombok.Getter
    private static class GoogleUserInfo {
        private String email;
        private String name;
        private String picture;
    }
}
