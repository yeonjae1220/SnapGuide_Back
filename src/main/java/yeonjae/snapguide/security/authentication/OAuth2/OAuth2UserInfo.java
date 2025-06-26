package yeonjae.snapguide.security.authentication.OAuth2;

import java.util.Map;

public interface OAuth2UserInfo {
    String getProvider(); // ex. "google", "naver", "kakao"
    String getProviderId(); // ex. Google: "sub", Naver: "id", Kakao: "id"
    String getEmail(); // 사용자 이메일

    String getName(); // 사용자 이름

    Map<String, Object> getAttributes(); // 원본 attribute map
}
