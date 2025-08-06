package yeonjae.snapguide.controller.initData;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import yeonjae.snapguide.domain.guide.Guide;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.domain.location.LocationDto;
import yeonjae.snapguide.domain.location.LocationMapper;
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
// 잠깐 꺼둠, Point type에 맞춰서 해야함
//    @PostConstruct
    public void init() {
        if (memberRepository.findByEmail("test1").isPresent()) {
            return;
        }
        if (memberRepository.findByEmail("test2").isPresent()) {
            return;
        }

        // 1. 테스트 회원 생성
        Member member1 = Member.builder()
                .email("test1")
                .password(passwordEncoder.encode("test"))
                .nickname("테스트유저1")
                .provider(Provider.LOCAL)
                .authority(List.of(Authority.MEMBER))
                .build();
        memberRepository.save(member1);

        Member member2 = Member.builder()
                .email("test2")
                .password(passwordEncoder.encode("test"))
                .nickname("테스트유저2")
                .provider(Provider.LOCAL)
                .authority(List.of(Authority.MEMBER))
                .build();
        memberRepository.save(member2);





        // 2. 한국 내 5개 테스트 가이드
        createGuidesForCountry(member1, 37.5, 127.0, "한국");
        // 3. 일본
        createGuidesForCountry(member1, 35.6895, 139.6917, "일본"); // 도쿄
        // 4. 중국
        createGuidesForCountry(member1, 31.2304, 121.4737, "중국"); // 상하이
        // 5. 러시아
        createGuidesForCountry(member2, 55.7558, 37.6173, "러시아"); // 모스크바
        // 6. 미국
        createGuidesForCountry(member2, 40.7128, -74.0060, "미국"); // 뉴욕
        // 7. 루마니아
        createGuidesForCountry(member2, 44.4268, 26.1025, "루마니아"); // 부쿠레슈티
        log.info("[InitTestData] : 다국적 테스트 데이터 생성 완료");



    }

    /**
     * 위도, 경도를 기준으로 5개의 가이드를 생성하는 유틸 메서드
     */
    private void createGuidesForCountry(Member member, double baseLat, double baseLng, String countryName) {
        for (int i = 0; i < 5; i++) {
            double lat = baseLat + (i * 0.01);
            double lng = baseLng + (i * 0.01);

            Location location = reverseGeocodingService.reverseGeocode(lat, lng).block();
            locationRepository.save(location);

            Guide guide = Guide.builder()
                    .author(member)
                    .location(location)
                    .tip(countryName + " 테스트 팁 #" + (i + 1))
                    .build();
            guideRepository.save(guide);
        }
    }

}








