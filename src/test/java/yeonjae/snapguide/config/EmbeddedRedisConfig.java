package yeonjae.snapguide.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import redis.embedded.RedisServer;
import redis.embedded.core.RedisServerBuilder;
import yeonjae.snapguide.service.PushService;

import java.io.IOException;

@Configuration
@ActiveProfiles("test")
public class EmbeddedRedisConfig {

    @MockBean
    @SuppressWarnings("unused")
    private PushService pushService;

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() throws IOException {
        redisServer = new RedisServerBuilder()
                .port(6370)
                .setting("maxmemory 16mb")
                .build();
        redisServer.start();
    }

    @PreDestroy
    public void stopRedis() throws IOException {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
        }
    }
}
