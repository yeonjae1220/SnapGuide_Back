package yeonjae.snapguide.repository;

import org.springframework.data.repository.CrudRepository;
import yeonjae.snapguide.security.authentication.OAuth2.OAuth2AuthorizationCode;

/**
 * OAuth2 Authorization Code를 Redis에 저장/조회하는 Repository
 */
public interface OAuth2AuthorizationCodeRepository extends CrudRepository<OAuth2AuthorizationCode, String> {
}
