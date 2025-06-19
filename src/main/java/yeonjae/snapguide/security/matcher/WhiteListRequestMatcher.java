package yeonjae.snapguide.security.matcher;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.List;
import java.util.stream.Collectors;

public class WhiteListRequestMatcher implements RequestMatcher {

    private final OrRequestMatcher matcher;

    /**
     * Matches requests that are not in the WhiteList.
     */
    @Override
    public boolean matches(HttpServletRequest request) {
        return !matcher.matches(request); // 화이트리스트에 포함되지 않은 요청에만 true
    }

    public WhiteListRequestMatcher(List<String> whiteList) {
        final List<RequestMatcher> requestMatchers = whiteList.stream()
                // 경로 패턴에 맞게 변환, /api/user/**, /login, /index.html 같은 패턴 매칭을 지원
                .map(AntPathRequestMatcher::new)
                .collect(Collectors.toList());
        this.matcher = new OrRequestMatcher(requestMatchers); // 여러 경로를 OR로 처리, 여러 개 중 하나라도 매칭되면 true
    }
}
