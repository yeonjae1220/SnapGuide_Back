package yeonjae.snapguide.security.authentication.OAuth2;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.HashMap;
import java.util.Map;

public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public CustomOAuth2AuthorizationRequestResolver(ClientRegistrationRepository repo, String baseUri) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(repo, baseUri);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return addPromptParam(delegate.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return addPromptParam(delegate.resolve(request, clientRegistrationId));
    }

    private OAuth2AuthorizationRequest addPromptParam(OAuth2AuthorizationRequest request) {
        if (request == null) return null;
        Map<String, Object> params = new HashMap<>(request.getAdditionalParameters());
        params.put("prompt", "select_account");
        return OAuth2AuthorizationRequest.from(request)
                .additionalParameters(params)
                .build();
    }
}
