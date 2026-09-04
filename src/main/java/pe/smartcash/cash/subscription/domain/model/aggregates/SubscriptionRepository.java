package pe.smartcash.cash.subscription.domain.model.aggregates;

import java.util.Optional;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionId;
import pe.smartcash.cash.subscription.domain.model.valueobjects.UserId;

public interface SubscriptionRepository {

  Optional<Subscription> findById(SubscriptionId id);

  Optional<Subscription> findActiveByUserId(UserId userId);

  /**
   * Los eventos de Stripe que sincronizan el ciclo de vida de una suscripción ya activada
   * ({@code invoice.paid}, {@code customer.subscription.deleted}) no traen nuestro
   * {@code userId} -- solo el id de la Subscription de Stripe. Por eso hace falta este índice
   * aparte de {@link #findActiveByUserId}.
   */
  Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

  void save(Subscription subscription);
}
