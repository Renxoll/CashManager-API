package pe.smartcash.cash.transactions.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import pe.smartcash.cash.transactions.domain.model.aggregates.PendingSender;
import pe.smartcash.cash.transactions.domain.model.aggregates.PendingSenderRepository;
import pe.smartcash.cash.transactions.domain.model.valueobjects.PendingSenderId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.PendingSenderStatus;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;
import pe.smartcash.cash.transactions.infrastructure.persistence.jpa.repositories.PendingSenderJpaRepository;

@Repository
class PendingSenderRepositoryAdapter implements PendingSenderRepository {

  private final PendingSenderJpaRepository jpaRepository;

  PendingSenderRepositoryAdapter(PendingSenderJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public void save(PendingSender pendingSender) {
    jpaRepository.save(PendingSenderEntityMapper.toJpaEntity(pendingSender));
  }

  @Override
  public Optional<PendingSender> findById(PendingSenderId id) {
    return jpaRepository.findById(id.value()).map(PendingSenderEntityMapper::toDomain);
  }

  @Override
  public Optional<PendingSender> findByUserIdAndDomain(UserId userId, String domain) {
    return jpaRepository.findByUserIdAndDomain(userId.value(), domain).map(PendingSenderEntityMapper::toDomain);
  }

  @Override
  public List<PendingSender> findAllPendingByUserId(UserId userId) {
    return jpaRepository.findAllByUserIdAndStatus(userId.value(), PendingSenderStatus.PENDING).stream()
        .map(PendingSenderEntityMapper::toDomain)
        .toList();
  }
}
