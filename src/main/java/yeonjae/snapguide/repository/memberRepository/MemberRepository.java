package yeonjae.snapguide.repository.memberRepository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeonjae.snapguide.domain.member.Member;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);

    /**
     * authority를 함께 조회 (N+1 방지)
     * 인증/JWT 생성 시 사용
     */
    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.authority WHERE m.email = :email")
    Optional<Member> findByEmailWithAuthority(@Param("email") String email);

//    Optional<Member> findByLoginId(String loginId);

    Boolean existsByEmail(String email);

}
