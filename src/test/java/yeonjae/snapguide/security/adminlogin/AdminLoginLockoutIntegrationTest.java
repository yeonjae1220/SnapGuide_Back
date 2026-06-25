package yeonjae.snapguide.security.adminlogin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * 실제 {@code adminFilterChain} + 임베디드 Redis로 검증하는 lockout end-to-end.
 *
 * <p>Mock 없이 실제 카운팅을 태운다: 같은 IP로 {@code ip-max-attempts}(기본 5)회
 * 로그인을 연속 실패시키면 그 다음 요청이 비밀번호 검증 전에 429로 차단되는지를
 * 확인한다. IP는 {@code ClientIpResolver}가 신뢰 프록시(127.0.0.1)에서 온
 * X-Forwarded-For를 신뢰해 해석하므로, 헤더로 합성 클라이언트 IP를 주입한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class AdminLoginLockoutIntegrationTest {

    private static final String CLIENT_IP = "198.51.100.77";
    private static final String OTHER_IP = "203.0.113.5";
    private static final String ADMIN = "admin@test.example.com";

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(wac).apply(springSecurity()).build();
        // 같은 JVM의 임베디드 Redis를 공유하므로 admin-login 카운터/락을 초기화해 테스트 격리
        Set<String> keys = redisTemplate.keys("admin:login:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("같은 IP에서 5회 연속 실패하면 6회째 POST는 429 + Retry-After 로 차단된다")
    void fiveFailures_sixthBlockedWith429() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/admin/login").with(csrf())
                            .header("X-Forwarded-For", CLIENT_IP)
                            .param("username", ADMIN)
                            .param("password", "wrong-" + i))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/login?error"));
        }

        mockMvc.perform(post("/admin/login").with(csrf())
                        .header("X-Forwarded-For", CLIENT_IP)
                        .param("username", ADMIN)
                        .param("password", "wrong-again"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    @DisplayName("한 IP의 lockout은 다른 IP에 영향을 주지 않는다 (per-IP 격리)")
    void lockoutIsScopedPerIp() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/admin/login").with(csrf())
                            .header("X-Forwarded-For", CLIENT_IP)
                            .param("username", ADMIN)
                            .param("password", "x" + i))
                    .andExpect(status().is3xxRedirection());
        }

        // 다른 IP는 락이 없으므로 정상 실패 처리(?error)되고 429가 아니다
        mockMvc.perform(post("/admin/login").with(csrf())
                        .header("X-Forwarded-For", OTHER_IP)
                        .param("username", ADMIN)
                        .param("password", "y"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?error"));
    }
}
