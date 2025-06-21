package yeonjae.snapguide.repository;

import org.springframework.data.repository.CrudRepository;
import yeonjae.snapguide.infrastructure.cache.redis.RedisRefreshToken;
import yeonjae.snapguide.security.authentication.jwt.RefreshToken;

import java.util.Optional;

public interface RedisRefreshTokenRepository extends CrudRepository<RedisRefreshToken, String> {
    Optional<RedisRefreshToken> findByKey(String token);
}
