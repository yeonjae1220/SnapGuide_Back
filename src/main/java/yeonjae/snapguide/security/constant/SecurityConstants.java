package yeonjae.snapguide.security.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityConstants {

	public static final String REQUEST_HEADER_AUTHORIZATION = "Authorization";
	public static final String ACCESS_TOKEN = "accessToken";

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class AuthenticationWhiteList {
		public static final String[] TEST_API = {"/test/**", "/api/auth/test"};
		public static final String[] SWAGGER_V3 = {"/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**", "/swagger-ui.html", "/swagger-ui-custom.html"};
		public static final String[] AUTH_API = {"/api/auth/**", "/api/**", "/api-docs", "/api-docs/**", "/graphiql", "/graphql"};
		public static final String[] USER_API = {"/api/user/members/check", "/api/user/members/register", "/api/user/members/login", "/login", "/index.html", "/"};
		public static final String[] LOCAL_LOGIN_API = {"/api/auth/signup", "/api/auth/login", "api/auth/reissue"};

		public static List<String> getAllPatterns() {
			final List<String> whiteList = new ArrayList<>();
			whiteList.addAll(Arrays.stream(TEST_API).toList());
			whiteList.addAll(Arrays.stream(SWAGGER_V3).toList());
			whiteList.addAll(Arrays.stream(AUTH_API).toList());
			whiteList.addAll(Arrays.stream(USER_API).toList());
			whiteList.addAll(Arrays.stream(LOCAL_LOGIN_API).toList());
			return whiteList;
		}

	}

}
