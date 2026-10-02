package pe.smartcash.cash.subscription.domain.exception;

/** El proveedor de pagos (Culqi, detrás de {@code SubscriptionPaymentGateway}) falló o rechazó la operación. */
public class PaymentGatewayException extends RuntimeException {

  public PaymentGatewayException(String message, Throwable cause) {
    super(message, cause);
  }
}
