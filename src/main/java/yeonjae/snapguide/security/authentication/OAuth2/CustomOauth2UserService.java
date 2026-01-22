package yeonjae.snapguide.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.domain.member.Authority;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.domain.member.Provider;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;
import yeonjae.snapguide.security.authentication.OAuth2.CustomOauth2UserDetails;
import yeonjae.snapguide.security.authentication.OAuth2.GoogleUserDetails;
import yeonjae.snapguide.security.authentication.OAuth2.OAuth2UserInfo;

import java.util.List;

/**
 * OAuth2 로그인 성공 후 사용자 정보를 처리하는 서비스 클래스.
 * DefaultOAuth2UserService를 상속받아 `loadUser` 메소드를 오버라이드하여 구현합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOauth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    /**
     * Spring Security가 OAuth2 Provider(예: 구글)로부터 Access Token을 받은 후 호출하는 메소드.
     * 이 메소드에서 Provider로부터 받은 사용자 정보를 가공하여 반환합니다.
     *
     * @param userRequest Provider로부터 받은 Access Token과 클라이언트 등록 정보가 담겨있습니다.
     * @return 가공된 사용자 정보가 담긴 OAuth2User 객체 (우리 서비스의 CustomOauth2UserDetails)
     * @throws OAuth2AuthenticationException 인증 과정에서 문제 발생 시
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 부모 클래스(DefaultOAuth2UserService)의 loadUser를 호출하여 Provider로부터 사용자 정보를 가져옵니다.
        // 이 oAuth2User 객체는 소셜 서비스에서 제공하는 원시 사용자 정보를 담고 있습니다.
        OAuth2User oAuth2User = super.loadUser(userRequest);
        log.info("Successfully loaded user attributes from OAuth2 provider: {}", oAuth2User.getAttributes());

        // 2. 어떤 소셜 로그인 서비스인지 구분합니다. (예: "google", "naver", "kakao")
        String provider = userRequest.getClientRegistration().getRegistrationId();

        // 3. 각 소셜 서비스의 특성에 맞게 사용자 정보를 파싱합니다.
        // 여기서는 switch 문을 사용하여 각 Provider별로 다른 구현체를 사용하도록 분기합니다.
        OAuth2UserInfo userInfo = switch (provider) {
            case "google" -> new GoogleUserDetails(oAuth2User.getAttributes());
            // TODO: 추후 네이버, 카카오 등 다른 소셜 로그인을 추가할 경우 여기에 case를 추가합니다.
            // case "naver" -> new NaverUserDetails(oAuth2User.getAttributes());
            default -> throw new OAuth2AuthenticationException("Unsupported social login provider: " + provider);
        };

        String userEmail = userInfo.getEmail();
        log.info("Parsed OAuth2 user info -> Email: {}, Provider: {}", userEmail, provider);

        // 4. 파싱된 이메일 정보를 바탕으로 DB에서 회원을 조회하거나 새로 가입시킵니다.
        Member member = saveOrUpdate(userInfo, provider);

        // 5. 최종적으로, 우리 서비스의 인증 객체인 CustomOauth2UserDetails를 생성하여 반환합니다.
        // 이 객체는 Spring Security의 SecurityContext에 저장되어 인증된 사용자로 관리됩니다.
        return new CustomOauth2UserDetails(member, oAuth2User.getAttributes());
    }

    /**
     * DB에 사용자가 존재하는지 확인하고, 존재하지 않으면 새로 저장(회원가입)하고,
     * 존재하면 정보를 업데이트하는 메소드.
     *
     * @param userInfo 파싱된 사용자 정보
     * @param provider 소셜 서비스 제공자 이름 (예: "google")
     * @return DB에 저장되거나 조회된 Member 엔티티
     */
    private Member saveOrUpdate(OAuth2UserInfo userInfo, String provider) {
        // ✅ authority를 함께 조회하여 N+1 방지 (OAuth2 인증 시 필요)
        Member member = memberRepository.findByEmailWithAuthority(userInfo.getEmail())
                // 이미 가입된 회원이라면, 이름이나 프로필 사진 등 변경될 수 있는 정보를 업데이트합니다.
                // .map(entity -> entity.update(userInfo.getName(), userInfo.getPicture())) // Member 엔티티에 update 메소드가 있다면 사용 가능

                // DB에 해당 이메일의 회원이 없다면, orElseGet 블록이 실행됩니다.
                .orElseGet(() -> {
                    log.info("New user detected. Creating a new member. Email: {}", userInfo.getEmail());
                    // Member 엔티티를 새로 생성하여 회원가입 처리
                    return memberRepository.save(Member.builder()
                            .nickname(userInfo.getName())
                            .email(userInfo.getEmail())
                            .provider(Provider.valueOf(provider.toUpperCase())) // "google" -> Provider.GOOGLE
                            .providerId(userInfo.getProviderId())
                            .authority(List.of(Authority.MEMBER)) // 신규 가입 시 기본으로 MEMBER 권한 부여
                            .build());
                });

        return member;
    }
}

