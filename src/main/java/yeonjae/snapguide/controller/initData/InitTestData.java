package yeonjae.snapguide.controller.initData;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import yeonjae.snapguide.domain.guide.Guide;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.domain.member.Authority;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.domain.member.Provider;
import yeonjae.snapguide.repository.guideRepository.GuideRepository;
import yeonjae.snapguide.repository.locationRepository.LocationRepository;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;
import yeonjae.snapguide.security.authentication.jwt.JwtTokenProvider;
import yeonjae.snapguide.service.ReverseGeocodingService;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InitTestData {
    private final MemberRepository memberRepository;
    private final LocationRepository locationRepository;
    private final GuideRepository guideRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final ReverseGeocodingService reverseGeocodingService;

    @PostConstruct
    public void init() {
        // 1. 테스트 회원 생성
        Member member = Member.builder()
                .email("test")
                .password(passwordEncoder.encode("test"))
                .nickname("테스트유저")
                .provider(Provider.LOCAL)
                .authority(List.of(Authority.MEMBER))
                .build();
        memberRepository.save(member);

        for (int i = 1; i <= 5; i++) {

            Location location = reverseGeocodingService.reverseGeocode(37.5 + (i * 0.01), 127.0 + (i * 0.01)).block();
            locationRepository.save(location);

            Guide guide = Guide.builder()
                    .author(member)
                    .location(location)
                    .tip("테스트 팁 #" + i)
                    .build();
            guideRepository.save(guide);
        }

        log.info("[InitTestData] : 테스트 데이터 생성 완료");
    }




}
