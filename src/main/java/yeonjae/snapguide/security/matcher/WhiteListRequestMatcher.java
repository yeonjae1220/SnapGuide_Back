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
        return !matcher.matches(request);
    }

    public WhiteListRequestMatcher(List<String> whiteList) {
        final List<RequestMatcher> requestMatchers = whiteList.stream()
                .map(AntPathRequestMatcher::new)
                .collect(Collectors.toList());
        this.matcher = new OrRequestMatcher(requestMatchers);
    }
}
