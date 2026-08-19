package pe.smartcash.cash.subscription.domain.services;

import pe.smartcash.cash.subscription.domain.model.commands.ActivateSubscriptionCommand;
import pe.smartcash.cash.subscription.domain.model.commands.CancelSubscriptionCommand;
import pe.smartcash.cash.subscription.domain.model.commands.StartCheckoutCommand;
import pe.smartcash.cash.subscription.domain.model.commands.SubscribeCommand;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionId;

public interface SubscriptionCommandService {

  /** Solo para planes sin costo (FREE): activa de inmediato, sin pasar por un gateway de pagos. */
  SubscriptionId handle(SubscribeCommand command);

  /** Planes pagos (PREMIUM): no activa nada, solo arranca el checkout y devuelve la URL. */
  CheckoutSession handle(StartCheckoutCommand command);

  /** Disparado por el webhook de Stripe tras un pago confirmado. Idempotente. */
  void handle(ActivateSubscriptionCommand command);

  void handle(CancelSubscriptionCommand command);
}
