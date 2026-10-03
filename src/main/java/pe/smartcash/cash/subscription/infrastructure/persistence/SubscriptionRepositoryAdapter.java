package pe.smartcash.cash.subscription.infrastructure.persistence;

import java.util.Optional;
import org.springframework.stereotype.Repository;
import pe.smartcash.cash.subscription.domain.model.aggregates.Subscription;
import pe.smartcash.cash.subscription.domain.model.aggregates.SubscriptionRepository;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionId;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionStatus;
import pe.smartcash.cash.subscription.domain.model.valueobjects.UserId;
import pe.smartcash.cash.subscription.infrastructure.persistence.jpa.repositories.SubscriptionSpringDataRepository;

@Repository
class SubscriptionRepositoryAdapter implements SubscriptionRepository {

  private final SubscriptionSpringDataRepository jpaRepository;

  SubscriptionRepositoryAdapter(SubscriptionSpringDataRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public Optional<Subscription> findById(SubscriptionId id) {
    return jpaRepository.findById(id.value()).map(SubscriptionEntityMapper::toDomain);
  }

  @Override
  public Optional<Subscription> findActiveByUserId(UserId userId) {
    return jpaRepository.findByUserIdAndStatus(userId.value(), SubscriptionStatus.ACTIVE).map(SubscriptionEntityMapper::toDomain);
  }

  @Override
  public Optional<Subscription> findByProviderSubscriptionId(String providerSubscriptionId) {
    return jpaRepository.findByProviderSubscriptionId(providerSubscriptionId).map(SubscriptionEntityMapper::toDomain);
  }

  @Override
  public void save(Subscription subscription) {
    // Flush inmediato a propósito: al hacer flush, Hibernate ejecuta los INSERT antes que los
    // UPDATE. En el upgrade FREE -> PREMIUM (cerrar el FREE y crear el PREMIUM en la misma
    // transacción) eso insertaría el PREMIUM con el FREE todavía ACTIVE y violaría el índice
    // único parcial "una sola suscripción ACTIVE por usuario".
    jpaRepository.saveAndFlush(SubscriptionEntityMapper.toJpaEntity(subscription));
  }
}
