package yeonjae.snapguide.springdoc;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;


@Configuration
public class SwaggerConfig {
    private static final String BEARER_TOKEN_PREFIX = "bearer";
    private static final String securityJwtName = "bearerAuth";
    /**
     * 내부에 openAPI() 빈의 설정에는 Security에 대한 설정을 해 주었다. 해당 설정은 JWT 인증을 사용할 경우를 가정하여 만든 것으로, JWT 인증에 사용될 Swagger 보안 스키마 규칙과 요구사항을 정의하였다. 최종적으로 openAPI 메서드에서는 보안 요구사항과 보안 스키마를 포함한 OpenAPI 객체를 생성하여 반환한다.
     * BEARER_TOKEN_PREFIX : JWT 토큰을 사용하는 보안 스키마의 접두사 규칙 설정에 사용되었다. "Bearer" Prefix를 사용하여 JWT를 전달한다고 설정하였다.
     */
    @Bean
    @Profile(value = {"local", "dev", "test"}) // Swagger를 운영환경에서는 사용 x
    // Swagger 문서의 전체적인 설정을 담고 있다.
    public OpenAPI openAPI() {
        // Swagger UI 접속 후, 딱 한 번만 accessToken을 입력해주면 모든 API에 토큰 인증 작업이 적용됩니다. (?)
        return new OpenAPI()
                .info(this.getApiInfo())
                .components(this.getSecuritySchemeComponents())
                .security(this.getSecurityRequirements());
    }

    private List<SecurityRequirement> getSecurityRequirements() {
        return List.of(new SecurityRequirement().addList(securityJwtName));
    }

    private Components getSecuritySchemeComponents() {
        final SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP).scheme(BEARER_TOKEN_PREFIX).bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER).name("Authorization");
        return new Components().addSecuritySchemes(securityJwtName, securityScheme);
    }

    private Info getApiInfo() {
        return new Info()
                .title("SnapGuide API Document")
                .version(APIVersions.VERSION_1_0_0.getVersion())
                .contact(this.getContact());
    }

    private Contact getContact() {
        return new Contact().name("Kim").email("duswokim1220@gmail.com");
    }

}
