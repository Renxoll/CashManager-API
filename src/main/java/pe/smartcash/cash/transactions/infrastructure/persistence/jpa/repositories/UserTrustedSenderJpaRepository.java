package pe.smartcash.cash.transactions.infrastructure.persistence.jpa.repositories;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.smartcash.cash.transactions.infrastructure.persistence.UserTrustedSenderJpaEntity;

public interface UserTrustedSenderJpaRepository extends JpaRepository<UserTrustedSenderJpaEntity, UUID> {

  boolean existsByUserIdAndDomain(UUID userId, String domain);
}
