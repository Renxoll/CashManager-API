package pe.smartcash.cash.subscription.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionStatus;

interface SubscriptionSpringDataRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {

  Optional<SubscriptionJpaEntity> findByUserIdAndStatus(UUID userId, SubscriptionStatus status);
}
