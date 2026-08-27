package pe.smartcash.cash.transactions.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import pe.smartcash.cash.transactions.domain.model.aggregates.UserTrustedSenderRepository;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;
import pe.smartcash.cash.transactions.infrastructure.persistence.jpa.repositories.UserTrustedSenderJpaRepository;

@Repository
class UserTrustedSenderRepositoryAdapter implements UserTrustedSenderRepository {

  private final UserTrustedSenderJpaRepository jpaRepository;

  UserTrustedSenderRepositoryAdapter(UserTrustedSenderJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean isTrusted(UserId userId, String domain) {
    return jpaRepository.existsByUserIdAndDomain(userId.value(), domain);
  }

  @Override
  public void trust(UserId userId, String domain, Instant trustedAt) {
    // Idempotente a propósito: (user_id, domain) tiene un índice único, y aprobar dos veces
    // el mismo remitente pendiente no debería nunca pasar (PendingSender.approve() exige
    // PENDING), pero mejor no depender de esa garantía acá también.
    if (jpaRepository.existsByUserIdAndDomain(userId.value(), domain)) {
      return;
    }
    jpaRepository.save(
        UserTrustedSenderJpaEntity.builder().id(UUID.randomUUID()).userId(userId.value()).domain(domain).trustedAt(trustedAt).build());
  }
}
