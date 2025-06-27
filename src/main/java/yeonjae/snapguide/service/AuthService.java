package yeonjae.snapguide.service;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.domain.member.dto.MemberRequestDto;
import yeonjae.snapguide.domain.member.dto.MemberResponseDto;
import yeonjae.snapguide.exception.CustomException;
import yeonjae.snapguide.exception.ErrorCode;
import yeonjae.snapguide.infrastructure.cache.redis.RedisRefreshToken;
import yeonjae.snapguide.repository.RedisRefreshTokenRepository;
import yeonjae.snapguide.repository.RefreshTokenRepository;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;
import yeonjae.snapguide.security.authentication.jwt.JwtToken;
import yeonjae.snapguide.security.authentication.jwt.JwtTokenProvider;
import yeonjae.snapguide.security.authentication.jwt.RefreshToken;
import yeonjae.snapguide.security.authentication.jwt.TokenRequestDto;

// https://velog.io/@jjeongdong/JWT-JWT%EB%A5%BC-%EC%82%AC%EC%9A%A9%ED%95%98%EC%97%AC-%EB%A1%9C%EA%B7%B8%EC%9D%B8-%ED%9A%8C%EC%9B%90%EA%B0%80%EC%9E%85-%EA%B5%AC%ED%98%84
@Slf4j
@Service
@RequiredArgsConstructor

public class AuthService {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisRefreshTokenRepository redisRefreshTokenRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordEncoder passwordEncoder;



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
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 3. 인증 정보를 기반으로 JWT 토큰 생성
        JwtToken jwtToken = jwtTokenProvider
                .generateToken(authentication.getAuthorities(),  // 권한 정보
                 authentication.getName());        // 사용자 식별자 여기서 pk인지 email인지?);

        // 4. RefreshToken 저장
        RedisRefreshToken refreshToken = RedisRefreshToken.builder()
                .key(authentication.getName())
                .value(jwtToken.getRefreshToken())
                .build();

        redisRefreshTokenRepository.save(refreshToken);

        // 5. 토큰 발급
        return jwtToken;
    }

    @Transactional
    public JwtToken reissue(TokenRequestDto tokenRequestDTO) {
        if (tokenRequestDTO.getAccessToken() == null || tokenRequestDTO.getAccessToken().isBlank()) {
            throw new CustomException(ErrorCode.TOKEN_NOT_FOUND);
        }

        // 1. Refresh Token 검증
        if (!jwtTokenProvider.validateToken(tokenRequestDTO.getRefreshToken())) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 2. Access Token 에서 Member ID 가져오기
        Authentication authentication = jwtTokenProvider.getAuthentication(tokenRequestDTO.getAccessToken());

        // 3. 저장소에서 Member ID 를 기반으로 Refresh Token 값 가져옴
        RedisRefreshToken refreshToken = redisRefreshTokenRepository.findByKey(authentication.getName())
                .orElseThrow(() -> new CustomException(ErrorCode.TOKEN_NOT_FOUND));

        // 4. Refresh Token 일치하는지 검사
        if (!refreshToken.getValue().equals(tokenRequestDTO.getRefreshToken())) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 5. 새로운 토큰 생성
        JwtToken jwtToken = null;
        // ✅ 기존 토큰들을 블랙리스트에 등록
        long accessTokenExpiry = jwtTokenProvider.getExpiration(tokenRequestDTO.getAccessToken());
        long refreshTokenExpiry = jwtTokenProvider.getExpiration(tokenRequestDTO.getRefreshToken());
        // 기존 accessToken 블랙 리스트에 추가
        tokenBlacklistService.blacklistAccessToken(tokenRequestDTO.getAccessToken(), accessTokenExpiry);
        if (jwtTokenProvider.refreshTokenPeriodCheck(refreshToken.getValue())) {
            jwtToken = jwtTokenProvider
                    .generateToken(authentication.getAuthorities(),  // 권한 정보
                            authentication.getName());        // 사용자 식별자 여기서 pk인지 email인지?);
            // 6. 저장소 정보 업데이트
            redisRefreshTokenRepository.save(refreshToken.updateValue(jwtToken.getRefreshToken()));
            // 기존 refreshToken 블랙 리스트에 추가
            tokenBlacklistService.blacklistRefreshToken(tokenRequestDTO.getRefreshToken(), refreshTokenExpiry);
        } else {
            // 5-2. Refresh Token의 유효기간이 3일 이상일 경우 Access Token만 재발급
            jwtToken = jwtTokenProvider.createAccessToken(authentication.getAuthorities(),  // 권한 정보
                    authentication.getName());
            // refreshToken은 보안상 재전달 x
        }

        // 토큰 발급
        return jwtToken;
    }

    @Transactional
    public void logout(TokenRequestDto tokenRequestDto) {
        String accessToken = tokenRequestDto.getAccessToken();

        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);
        String email = authentication.getName();

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

    }

}
