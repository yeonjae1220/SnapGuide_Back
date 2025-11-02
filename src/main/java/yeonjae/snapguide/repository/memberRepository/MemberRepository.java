package yeonjae.snapguide.repository.memberRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import yeonjae.snapguide.domain.member.Member;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);

//    Optional<Member> findByLoginId(String loginId);

    Boolean existsByEmail(String email);

}
