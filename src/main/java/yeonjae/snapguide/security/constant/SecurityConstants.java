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
		public static final String[] TEST_API = {"/test/**", "/api/auth/test", "/guide/api/nearby/**"}; //
		public static final String[] SWAGGER_V3 = {"/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**", "/swagger-ui.html", "/swagger-ui-custom.html"};

		public static final String[] AUTH_API = {"/api-docs", "/api-docs/**", "/graphiql", "/graphql"}; // "/api/**", "/api/auth/**",

		public static final String[] USER_API = {"/api/user/members/check", "/api/user/members/register", "/api/user/members/login", "/login", "/index.html", "/", "/test-api.html"};
		public static final String[] LOCAL_LOGIN_API = {"/api/auth/signup", "/api/auth/login", "/api/auth/reissue"};

		public static final String[] OAUTH_API = {"/oauth-login/admin", "/oauth-login/info", "/oauth/**", "/oauth-login/**", "/login", "/signup", "/oauth2/**", "/oauth2/redirect", "/error"};
		/**
		 * /.well-known/appspecific/...은 RFC 8615에서 정의된 “Well-Known URIs” 규약을 따르는 URL입니다. 브라우저나 앱, 확장 프로그램 등이 특정 정보를 찾기 위해 자동으로 요청합니다.
		 * 브라우저에서 dev tool 켜놓고 실행하니 자꾸 필터 검사 해서 화이트 리스트에 넣음
		 * 해당 경로는 인증과 무관하므로 다음과 같이 허용해주는 것이 일반적이라고 해서 넣었음
		 */
		public static final String[] DEV_TOOL = {"/.well-known/**", "/favicon.ico", "/css/**", "/js/**", "/images/**"};

		public static final String[] FILE_IO = {"/Users/kim-yeonjae/Desktop/Study/snapguide/uploads/**", "/uploads/**", "/media/**"};



		public static List<String> getAllPatterns() {
			final List<String> whiteList = new ArrayList<>();
			whiteList.addAll(Arrays.stream(TEST_API).toList());
			whiteList.addAll(Arrays.stream(SWAGGER_V3).toList());
			whiteList.addAll(Arrays.stream(AUTH_API).toList());
			whiteList.addAll(Arrays.stream(USER_API).toList());
			whiteList.addAll(Arrays.stream(LOCAL_LOGIN_API).toList());
			whiteList.addAll(Arrays.stream(OAUTH_API).toList());
			whiteList.addAll(Arrays.stream(DEV_TOOL).toList());
			whiteList.addAll(Arrays.stream(FILE_IO).toList());
			return whiteList;
		}

	}

}
