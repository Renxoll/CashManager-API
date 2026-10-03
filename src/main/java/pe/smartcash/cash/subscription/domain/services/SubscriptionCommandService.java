package pe.smartcash.cash.subscription.domain.services;

import pe.smartcash.cash.subscription.domain.model.commands.CancelSubscriptionCommand;
import pe.smartcash.cash.subscription.domain.model.commands.HandlePaymentEventCommand;
import pe.smartcash.cash.subscription.domain.model.commands.PayPremiumCommand;
import pe.smartcash.cash.subscription.domain.model.commands.SubscribeCommand;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionId;

public interface SubscriptionCommandService {

  /** Solo para planes sin costo (FREE): activa de inmediato, sin pasar por el proveedor de pagos. */
  SubscriptionId handle(SubscribeCommand command);

  /**
   * Planes pagos: cobra con la tarjeta tokenizada y activa en el momento si el proveedor lo
   * acepta. Devuelve un outcome en vez del id (excepción al patrón de comandos del proyecto)
   * porque "el banco pide 3DS" no es un error pero tampoco crea nada.
   *
   * @throws pe.smartcash.cash.subscription.domain.exception.PaymentDeclinedException si la tarjeta es rechazada.
   */
  PremiumPaymentOutcome handle(PayPremiumCommand command);

  void handle(CancelSubscriptionCommand command);

  /** Webhook del proveedor: verifica el evento contra su API y renueva o expira según corresponda. Idempotente. */
  void handle(HandlePaymentEventCommand command);
}
