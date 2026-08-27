package pe.smartcash.cash.transactions.infrastructure.persistence.jpa.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.smartcash.cash.transactions.domain.model.valueobjects.PendingSenderStatus;
import pe.smartcash.cash.transactions.infrastructure.persistence.PendingSenderJpaEntity;

public interface PendingSenderJpaRepository extends JpaRepository<PendingSenderJpaEntity, UUID> {

  Optional<PendingSenderJpaEntity> findByUserIdAndDomain(UUID userId, String domain);

  List<PendingSenderJpaEntity> findAllByUserIdAndStatus(UUID userId, PendingSenderStatus status);
}
