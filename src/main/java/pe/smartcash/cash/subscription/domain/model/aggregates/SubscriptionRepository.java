package pe.smartcash.cash.subscription.domain.model.aggregates;

import java.util.Optional;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionId;
import pe.smartcash.cash.subscription.domain.model.valueobjects.UserId;

public interface SubscriptionRepository {

  Optional<Subscription> findById(SubscriptionId id);

  Optional<Subscription> findActiveByUserId(UserId userId);

  void save(Subscription subscription);
}
