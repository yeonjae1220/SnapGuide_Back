package yeonjae.snapguide.security.authentication.OAuth2;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import yeonjae.snapguide.domain.member.Authority;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.domain.member.Provider;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Getter
public class CustomOauth2UserService extends DefaultOAuth2UserService {
    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        log.info("getAttributes : {}",oAuth2User.getAttributes());

        String provider = userRequest.getClientRegistration().getRegistrationId();


        // 뒤에 진행할 다른 소셜 서비스 로그인을 위해 구분 => 구글
        OAuth2UserInfo userInfo = switch (provider) {
            case "google" -> new GoogleUserDetails(oAuth2User.getAttributes());
            default -> throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인");
        };

        String providerId = userInfo.getProviderId();
        String email = userInfo.getEmail();
        String nickname = userInfo.getName();
//        String loginId = provider + "_" + providerId;
        log.info("OAuth2 email: {}", email);

        Member member = memberRepository.findByEmail(email)
                .orElseGet(() -> memberRepository.save(Member.builder()
                        .nickname(nickname)
                        .email(email)
                        .provider(Provider.valueOf(provider.toUpperCase())) // enum 타입 위해 이렇게 설정
                        .providerId(providerId)
                        .authority(List.of(Authority.MEMBER))  // TODO : 추후 권한은 멤버 하나당 하나만 가질 수 있게 수정?
                        .build()));

        return new CustomOauth2UserDetails(member, oAuth2User.getAttributes());
    }
}
