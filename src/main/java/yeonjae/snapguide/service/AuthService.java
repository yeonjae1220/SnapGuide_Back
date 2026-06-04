package yeonjae.snapguide.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.domain.member.Authority;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.domain.member.dto.MemberRequestDto;
import yeonjae.snapguide.domain.member.dto.MemberResponseDto;
import yeonjae.snapguide.exception.CustomException;
import yeonjae.snapguide.exception.ErrorCode;
import yeonjae.snapguide.infrastructure.cache.redis.RedisRefreshToken;
import yeonjae.snapguide.repository.RedisRefreshTokenRepository;
import yeonjae.snapguide.repository.RefreshTokenRepository;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;
import yeonjae.snapguide.repository.OAuth2AuthorizationCodeRepository;
import yeonjae.snapguide.security.authentication.OAuth2.OAuth2AuthorizationCode;
import yeonjae.snapguide.security.authentication.jwt.JwtToken;
import yeonjae.snapguide.security.authentication.jwt.JwtTokenProvider;
import yeonjae.snapguide.security.authentication.jwt.RefreshToken;
import yeonjae.snapguide.security.authentication.jwt.TokenHashUtil;
import yeonjae.snapguide.security.authentication.jwt.TokenRequestDto;
import yeonjae.snapguide.service.memberSerivce.MemberService;
import io.jsonwebtoken.Claims;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;


// https://velog.io/@jjeongdong/JWT-JWT%EB%A5%BC-%EC%82%AC%EC%9A%A9%ED%95%98%EC%97%AC-%EB%A1%9C%EA%B7%B8%EC%9D%B8-%ED%9A%8C%EC%9B%90%EA%B0%80%EC%9E%85-%EA%B5%AC%ED%98%84
@Slf4j
@Service
@RequiredArgsConstructor

public class AuthService {
    private final AuthenticationManager authenticationManager; // @Primary apiAuthenticationManager 주입
    private final MemberRepository memberRepository;
//    private final MemberService memberService; // NOTE : 나중에 멤버 서비스 쪽으로 다 옮겨야 하나? 역할이 좀 분산되네
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisRefreshTokenRepository redisRefreshTokenRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordEncoder passwordEncoder;
    private final OAuth2AuthorizationCodeRepository authCodeRepository;



    @Transactional
    public MemberResponseDto signup(MemberRequestDto request) {
        if (memberRepository.existsByEmail(request.getEmail())) {
             throw new CustomException(ErrorCode.DUPLICATE_USER);
        }
        Member member = request.toEntity(passwordEncoder);
        return MemberResponseDto.of(memberRepository.save(member));
    }

    @Transactional
    public JwtToken login(MemberRequestDto request) {
        // 1. Login ID/PW 를 기반으로 AuthenticationToken 생성
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());
        // 2. AuthenticationManager로 실제로 검증 (사용자 비밀번호 체크) 이 이루어지는 부분
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        // 3. 인증 정보를 기반으로 JWT 토큰 생성
        JwtToken jwtToken = jwtTokenProvider
                .generateToken(authentication.getAuthorities(),  // 권한 정보
                 authentication.getName());        // 사용자 식별자 여기서 pk인지 email인지?);

        // 4. RefreshToken 해시 후 저장 (원문 노출 방지)
        RedisRefreshToken refreshToken = RedisRefreshToken.builder()
                .key(authentication.getName())
                .value(TokenHashUtil.sha256(jwtToken.getRefreshToken()))
                .build();

        redisRefreshTokenRepository.save(refreshToken);

        // 5. 토큰 발급
        return jwtToken;
    }

    @Transactional
    public JwtToken reissue(TokenRequestDto tokenRequestDTO) {
        // 1. Refresh Token 검증
        if (!jwtTokenProvider.validateToken(tokenRequestDTO.getRefreshToken())) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 2. Cookie 기반 복원을 위해 Refresh Token subject로 사용자 세션 조회
        String userId = jwtTokenProvider.getSubject(tokenRequestDTO.getRefreshToken());
        if (userId == null || userId.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        RedisRefreshToken refreshToken = redisRefreshTokenRepository.findByKey(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TOKEN_NOT_FOUND));
        if (!refreshToken.getValue().equals(TokenHashUtil.sha256(tokenRequestDTO.getRefreshToken()))) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 3. 사용자 권한 정보 조회
        Member member = memberRepository.findByEmailWithAuthority(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Collection<? extends GrantedAuthority> authorities = member.getAuthority();

        // 4. 기존 Access Token이 전달된 경우에만 블랙리스트 등록
        blacklistAccessTokenIfPresent(tokenRequestDTO.getAccessToken());

        // 5. ToneBridge와 동일하게 재발급 시 Refresh Token도 함께 회전
        JwtToken jwtToken = jwtTokenProvider.generateToken(authorities, userId);
        redisRefreshTokenRepository.save(refreshToken.updateValue(TokenHashUtil.sha256(jwtToken.getRefreshToken())));

        long refreshTokenExpiry = jwtTokenProvider.getExpiration(tokenRequestDTO.getRefreshToken());
        if (refreshTokenExpiry > 0) {
            tokenBlacklistService.blacklistRefreshToken(tokenRequestDTO.getRefreshToken(), refreshTokenExpiry);
            log.info("기존 Refresh Token 블랙리스트 등록 완료 (TTL: {}ms)", refreshTokenExpiry);
        }

        // 토큰 발급
        return jwtToken;
    }

    private void blacklistAccessTokenIfPresent(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            log.info("Access Token 미전달 - 블랙리스트 등록 스킵");
            return;
        }

        try {
            long accessTokenExpiry = jwtTokenProvider.getExpiration(accessToken);
            if (accessTokenExpiry > 0) {
                tokenBlacklistService.blacklistAccessToken(accessToken, accessTokenExpiry);
                log.info("기존 Access Token 블랙리스트 등록 완료 (TTL: {}ms)", accessTokenExpiry);
            } else {
                log.info("Access Token 이미 만료됨 - 블랙리스트 등록 스킵");
            }
        } catch (CustomException e) {
            log.info("Access Token 파싱 실패 - Refresh Token 기반 재발급 계속 진행: {}", e.getErrorCode());
        }
    }

    @Transactional
    public String logout(TokenRequestDto tokenRequestDto) {
        String accessToken = tokenRequestDto.getAccessToken();

        Claims claims;
        try {
            claims = jwtTokenProvider.parseExpiredToken(accessToken);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        String email = claims.getSubject();

        // 1. AccessToken 블랙리스트 등록 (만료 시각까지 TTL 설정)
        long accessTokenExpiration = jwtTokenProvider.getExpiration(accessToken);
        tokenBlacklistService.blacklistAccessToken(accessToken, accessTokenExpiration);

        // 2. RefreshToken 블랙리스트 등록 (옵션)
        String refreshToken = tokenRequestDto.getRefreshToken();
        if (jwtTokenProvider.validateToken(refreshToken)) {
            long refreshTokenExpiration = jwtTokenProvider.getExpiration(refreshToken);
            tokenBlacklistService.blacklistRefreshToken(refreshToken, refreshTokenExpiration);
        }

        // 3. Redis에서 RefreshToken 삭제
        redisRefreshTokenRepository.deleteById(email);
        return email;
    }

    /**
     * OAuth2 Authorization Code를 JWT 토큰으로 교환
     * - 모바일 앱에서 받은 일회용 code를 검증하고 토큰 발급
     * - code는 사용 후 즉시 삭제됨 (재사용 불가)
     */
    @Transactional
    public JwtToken exchangeOAuth2Code(String code) {
        // 1. Redis에서 code 조회
        OAuth2AuthorizationCode authCode = authCodeRepository.findById(code)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

        String email = authCode.getEmail();

        // 2. code 즉시 삭제 (일회용)
        authCodeRepository.deleteById(code);
        log.info("Authorization code 사용 및 삭제: {} for user: {}", code, email);

        // 3. 사용자 정보 조회 (✅ authority 함께 조회하여 N+1 방지)
        Member member = memberRepository.findByEmailWithAuthority(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 4. JWT 토큰 생성 (OAuth2 코드 교환도 Google OAuth — ADMIN 권한 제외)
        java.util.List<Authority> jwtAuthorities = member.getAuthority().stream()
                .filter(a -> a != Authority.ADMIN)
                .toList();
        JwtToken jwtToken = jwtTokenProvider.generateToken(jwtAuthorities, member.getEmail());

        // 5. RefreshToken 해시 후 저장 (원문 노출 방지)
        RedisRefreshToken refreshToken = RedisRefreshToken.builder()
                .key(email)
                .value(TokenHashUtil.sha256(jwtToken.getRefreshToken()))
                .build();
        redisRefreshTokenRepository.save(refreshToken);

        log.info("OAuth2 code 교환 성공 - 토큰 발급 for user: {}", email);

        return jwtToken;
    }
}
