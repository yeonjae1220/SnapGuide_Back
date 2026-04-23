package yeonjae.snapguide.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yeonjae.snapguide.domain.push.PushSubscription;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    Optional<PushSubscription> findByEndpoint(String endpoint);

    List<PushSubscription> findAllByMemberIdAndActiveTrue(Long memberId);

    List<PushSubscription> findAllByActiveTrue();

    void deleteByMemberId(Long memberId);
}
