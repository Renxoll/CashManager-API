package pe.smartcash.cash.subscription.domain.model.aggregates;

import java.util.Optional;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionId;
import pe.smartcash.cash.subscription.domain.model.valueobjects.UserId;

public interface SubscriptionRepository {

  Optional<Subscription> findById(SubscriptionId id);

  Optional<Subscription> findActiveByUserId(UserId userId);

  /**
   * Los webhooks del proveedor de pagos (cobro del período siguiente, baja por reintentos
   * agotados) no traen nuestro {@code userId} -- solo el id de la suscripción en el proveedor.
   * Por eso hace falta este índice aparte de {@link #findActiveByUserId}.
   */
  Optional<Subscription> findByProviderSubscriptionId(String providerSubscriptionId);

  void save(Subscription subscription);
}
