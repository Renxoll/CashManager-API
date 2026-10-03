package pe.smartcash.cash.subscription.domain.exception;

/**
 * El proveedor de pagos rechazó la tarjeta o el cobro. El mensaje es el que el proveedor
 * redacta para el titular, así que se puede mostrar tal cual en la app.
 */
public class PaymentDeclinedException extends RuntimeException {

  public PaymentDeclinedException(String userMessage) {
    super(userMessage);
  }
}
