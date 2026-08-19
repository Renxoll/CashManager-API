package pe.smartcash.cash.subscription.infrastructure.persistence.jpa.repositories;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionStatus;
import pe.smartcash.cash.subscription.infrastructure.persistence.SubscriptionJpaEntity;

public interface SubscriptionSpringDataRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {

  Optional<SubscriptionJpaEntity> findByUserIdAndStatus(UUID userId, SubscriptionStatus status);
}
