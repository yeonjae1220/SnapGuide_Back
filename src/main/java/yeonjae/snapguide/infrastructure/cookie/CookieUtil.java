package yeonjae.snapguide.infrastructure.cookie;

import org.springframework.util.SerializationUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Base64;
import java.util.Optional;

/**
 * 쿠키 관련 작업을 도와주는 유틸리티 클래스입니다.
 */
public class CookieUtil {
    /**
     * 요청(request)에 포함된 쿠키 배열에서 원하는 이름의 쿠키를 찾아 반환합니다.
     * @param request HttpServletRequest 객체
     * @param name 찾고자 하는 쿠키의 이름
     * @return Optional<Cookie> 지정된 이름의 쿠키 객체 (없을 경우 Optional.empty())
     */
    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 응답(response)에 쿠키를 추가합니다.
     * @param response HttpServletResponse 객체
     * @param name 쿠키의 이름
     * @param value 쿠키의 값
     * @param maxAge 쿠키의 유효 시간 (초 단위)
     */
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/"); // 모든 경로에서 접근 가능하도록 설정
        cookie.setHttpOnly(true); // JavaScript를 통한 접근 방지 (XSS 보호)
        cookie.setMaxAge(maxAge); // 유효 시간 설정
        response.addCookie(cookie);
    }

    /**
     * 요청과 응답 객체에서 특정 이름의 쿠키를 삭제합니다.
     * 쿠키의 유효 시간을 0으로 설정하여 브라우저가 즉시 삭제하도록 합니다.
     * @param request HttpServletRequest 객체
     * @param response HttpServletResponse 객체
     * @param name 삭제하고자 하는 쿠키의 이름
     */
    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0); // 유효 시간을 0으로 설정하여 쿠키 삭제
                    response.addCookie(cookie);
                }
            }
        }
    }

    /**
     * 객체를 직렬화하여 쿠키 값으로 사용할 수 있는 문자열로 변환합니다.
     * (URL-safe Base64 인코딩 사용)
     * @param obj 직렬화할 객체
     * @return Base64로 인코딩된 문자열
     */
    public static String serialize(Object obj) {
        return Base64.getUrlEncoder()
                .encodeToString(SerializationUtils.serialize(obj));
    }

    /**
     * 쿠키 값(문자열)을 역직렬화하여 원래의 객체로 복원합니다.
     * @param cookie 역직렬화할 쿠키 객체
     * @param cls 복원할 객체의 클래스 타입
     * @return 역직렬화된 객체
     */
    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        return cls.cast(SerializationUtils.deserialize(
                        Base64.getUrlDecoder().decode(cookie.getValue())
                )
        );
    }
}
