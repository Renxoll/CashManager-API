package pe.smartcash.cash.subscription.domain.services;

import pe.smartcash.cash.subscription.domain.exception.PaymentGatewayException;
import pe.smartcash.cash.subscription.domain.model.valueobjects.PlanCode;
import pe.smartcash.cash.subscription.domain.model.valueobjects.UserId;

/**
 * Puerto hacia el proveedor de pagos. La implementación por defecto (infrastructure.payment)
 * habla con Stripe Checkout, pero el resto del contexto solo depende de esta interfaz —
 * ningún tipo de {@code com.stripe.*} cruza hacia domain o application.
 */
public interface SubscriptionPaymentGateway {

  /** @throws PaymentGatewayException si el proveedor de pagos falla o rechaza la operación. */
  CheckoutSession startCheckout(UserId userId, PlanCode planCode);

  /**
   * Cancela del lado del proveedor de pagos la suscripción recurrente identificada por {@code
   * stripeSubscriptionId} -- sin esto, cancelar solo en nuestra BD no detiene el cobro
   * automático de cada ciclo. Idempotente: cancelar una suscripción que el proveedor ya tiene
   * como cancelada no debe fallar (reintentos del caller no deberían romperse).
   *
   * @throws PaymentGatewayException si el proveedor de pagos falla o rechaza la operación.
   */
  void cancel(String stripeSubscriptionId);
}
