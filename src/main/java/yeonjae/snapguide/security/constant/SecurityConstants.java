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
		public static final String[] TEST_API = {"/test/**"}; // "/api/auth/test"
		// 비인증 공개 접근 허용 가이드 엔드포인트 (JWT 처리 대상에서 제외)
		public static final String[] GUIDE_PUBLIC_API = {"/guide/api/nearby", "/guide/api/aggregate"};
		public static final String[] SWAGGER_V3 = {"/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**", "/swagger-ui.html", "/swagger-ui-custom.html"};

		public static final String[] AUTH_API = {"/api-docs", "/api-docs/**"}; // "/api/**", "/api/auth/**",

		public static final String[] USER_API = {"/login", "/index.html", "/", "/test-api.html", "/admin.html", "/admin"};
		public static final String[] LOCAL_LOGIN_API = {"/api/auth/signup", "/api/auth/login", "/api/auth/reissue", "/api/auth/google/token", "/api/auth/oauth/token"};

		public static final String[] OAUTH_API = {"/oauth-login/admin", "/oauth-login/info", "/oauth/**", "/oauth-login/**", "/login", "/signup", "/oauth2/**", "/oauth2/redirect", "/error"};
		/**
		 * /.well-known/appspecific/...은 RFC 8615에서 정의된 “Well-Known URIs” 규약을 따르는 URL입니다. 브라우저나 앱, 확장 프로그램 등이 특정 정보를 찾기 위해 자동으로 요청합니다.
		 * 브라우저에서 dev tool 켜놓고 실행하니 자꾸 필터 검사 해서 화이트 리스트에 넣음
		 * 해당 경로는 인증과 무관하므로 다음과 같이 허용해주는 것이 일반적이라고 해서 넣었음
		 */
		public static final String[] DEV_TOOL = {"/.well-known/**", "/favicon.ico", "/css/**", "/js/**", "/images/**"};

		// WARN fix: /uploads, /media를 JWT 화이트리스트에서 제거
		// - /uploads/**는 WebConfig에서 정적 서빙이 주석 처리됨 (실제 미동작)
		// - /media/** API는 SecurityConfig에서 인증 필요로 처리
		// public static final String[] FILE_IO = {"/uploads/**", "/media/**"};

		// k8s probe 경로만 JWT 필터 우회 — JWT 필터가 Spring Security permitAll()보다 먼저 실행되므로
		// /actuator/health/liveness, /readiness 에 토큰 없이 접근 가능해야 한다
		// /actuator/** 전체가 아닌 health 하위 경로만 허용 (metrics 등 노출 방지)
		public static final String[] ACTUATOR_HEALTH = {"/actuator/health", "/actuator/health/**"};

		public static final String[] LOCATION_API = {"/location/api/places/**"};

		public static final String[] PWA_PUBLIC = {
				"/manifest.json",
				"/service-worker.js",
				"/offline.html",
				"/icons/**",
				"/api/push/vapid-public-key"
		};

		// 파일명이 UUID라 추측 불가능, S3 presigned URL로 리다이렉트되어 10분 만료 적용
		public static final String[] MEDIA_FILES = {"/media/files/**"};

		// 공개 지도 탐색에 필요한 Maps 엔드포인트
		public static final String[] MAPS_PUBLIC = {"/api/maps/key", "/api/maps/location"};

		public static List<String> getAllPatterns() {
			final List<String> whiteList = new ArrayList<>();
			whiteList.addAll(Arrays.stream(TEST_API).toList());
			whiteList.addAll(Arrays.stream(GUIDE_PUBLIC_API).toList());
			whiteList.addAll(Arrays.stream(SWAGGER_V3).toList());
			whiteList.addAll(Arrays.stream(AUTH_API).toList());
			whiteList.addAll(Arrays.stream(USER_API).toList());
			whiteList.addAll(Arrays.stream(LOCAL_LOGIN_API).toList());
			whiteList.addAll(Arrays.stream(OAUTH_API).toList());
			whiteList.addAll(Arrays.stream(DEV_TOOL).toList());
			whiteList.addAll(Arrays.stream(ACTUATOR_HEALTH).toList());
			whiteList.addAll(Arrays.stream(LOCATION_API).toList());
			whiteList.addAll(Arrays.stream(PWA_PUBLIC).toList());
			whiteList.addAll(Arrays.stream(MEDIA_FILES).toList());
			whiteList.addAll(Arrays.stream(MAPS_PUBLIC).toList());
			return whiteList;
		}

	}

}
