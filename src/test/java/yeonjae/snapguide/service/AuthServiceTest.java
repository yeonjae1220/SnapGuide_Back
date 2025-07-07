package yeonjae.snapguide.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.domain.member.dto.MemberRequestDto;
import yeonjae.snapguide.infrastructure.cache.redis.RedisRefreshToken;
import yeonjae.snapguide.repository.RedisRefreshTokenRepository;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;
import yeonjae.snapguide.security.authentication.jwt.JwtToken;
import yeonjae.snapguide.security.authentication.jwt.JwtTokenProvider;
import yeonjae.snapguide.security.authentication.jwt.TokenRequestDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Transactional
class AuthServiceTest {
    @Autowired
    AuthService authService;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    RedisRefreshTokenRepository redisRefreshTokenRepository;
    @Autowired
    JwtTokenProvider jwtTokenProvider;
    @Autowired
    PasswordEncoder passwordEncoder;

    private final String email = "test@example.com";
    private final String password = "dummyPassword";
    private final String nickname = "dummyPassword";

    @BeforeEach
    void setUp() {
        MemberRequestDto signupDto = new MemberRequestDto();
//        signupDto.setEmail(email);
//        signupDto.setPassword(password);
        ReflectionTestUtils.setField(signupDto, "email", "test@example.com"); // private 필드에 값을 직접 주입할 수 있도록 도와주는 유틸리티
        ReflectionTestUtils.setField(signupDto, "password", "dummyPassword");
        ReflectionTestUtils.setField(signupDto, "nickname", "dummyNickname");
//        ReflectionTestUtils.setField(signupDto, "authority", "MEMBER");

        authService.signup(signupDto);
    }

    @Test
    void get_token_after_login() {
        // given
        MemberRequestDto loginDto = new MemberRequestDto(email, password, nickname, null);

        // when
        JwtToken token = authService.login(loginDto);

        // then
        assertThat(token.getAccessToken()).isNotNull();
        assertThat(token.getRefreshToken()).isNotNull();

        RedisRefreshToken redisToken = redisRefreshTokenRepository.findByKey(email)
                .orElseThrow();
        assertThat(redisToken.getValue()).isEqualTo(token.getRefreshToken());
    }

    @Test
    void issue_refreshToken_n_accessToken_less_3days() throws InterruptedException {
        // given
        JwtToken token = authService.login(new MemberRequestDto(email, password, nickname, null));
        TokenRequestDto dto = new TokenRequestDto();
        ReflectionTestUtils.setField(dto, "accessToken", token.getAccessToken());
        ReflectionTestUtils.setField(dto, "refreshToken", token.getRefreshToken());
        // 최소한 1ms 정도 시간 차를 두면 issuedAt이 달라져 JWT 전체가 달라짐
        // 이거 없으면 아래 엑세스 토큰 비교 값 동일하게 나온다
        Thread.sleep(1000);  // or delay using clock mocking

        // 4일 이상 남은 상태로 설정되어 있어야 함
        JwtToken reissued = authService.reissue(dto);

        // then
        // 블랙리스트 등록은 외부 Redis 확인 필요. 테스트 환경에서는 assert 불가 라는 데용
        assertThat(reissued.getAccessToken()).isNotEqualTo(token.getAccessToken()); // 새로 발급됨
        assertThat(reissued.getRefreshToken()).isNull(); // 생성안함
    }

    @Test
    void logout_n_token_blacklist_n_refreshToken_delete() {
        JwtToken token = authService.login(new MemberRequestDto(email, password, nickname, null));
        TokenRequestDto dto = new TokenRequestDto();
        ReflectionTestUtils.setField(dto, "accessToken", token.getAccessToken());
        ReflectionTestUtils.setField(dto, "refreshToken", token.getRefreshToken());

        authService.logout(dto);

        assertThat(redisRefreshTokenRepository.findByKey(email)).isEmpty();
        // 블랙리스트 등록은 외부 Redis 확인 필요. 테스트 환경에서는 assert 불가
    }

}