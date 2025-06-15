package yeonjae.snapguide.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.security.authentication.jwt.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
//    @Override
//    Optional<RefreshToken> findById(Long id);
    Optional<RefreshToken> findByKey(String token);
}
