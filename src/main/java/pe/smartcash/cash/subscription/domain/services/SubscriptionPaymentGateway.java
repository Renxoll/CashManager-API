package pe.smartcash.cash.subscription.domain.services;

import java.util.Optional;
import pe.smartcash.cash.subscription.domain.exception.PaymentGatewayException;

/**
 * Puerto hacia el proveedor de pagos. La implementación (infrastructure.payment) habla con
 * Culqi, pero el resto del contexto solo depende de esta interfaz: ningún detalle del API de
 * Culqi (ids {@code cus_}/{@code crd_}/{@code sxn_}, códigos de estado, formato de errores)
 * cruza hacia domain o application.
 */
public interface SubscriptionPaymentGateway {

  /**
   * Suscribe la tarjeta tokenizada al plan pedido. Un rechazo de la tarjeta o un pedido de 3DS
   * son resultados normales ({@link PaymentResult}), no excepciones.
   *
   * @throws PaymentGatewayException si el proveedor falla (caído, credenciales inválidas, error inesperado).
   */
  PaymentResult subscribe(PaymentRequest request);

  /**
   * Cancela la suscripción recurrente en el proveedor: sin esto, cancelar solo en nuestra BD no
   * detiene el cobro de cada período. Idempotente: cancelar una que ya está cancelada no falla.
   *
   * @throws PaymentGatewayException si el proveedor falla o rechaza la operación.
   */
  void cancel(String providerSubscriptionId);

  /**
   * Lee el evento {@code eventId} directo de la API del proveedor (con nuestra llave secreta) y
   * lo traduce. Es la verificación real de un webhook: el proveedor no firma sus webhooks, así
   * que nada del cuerpo recibido se usa. Vacío si el evento no existe o es de un tipo que no
   * afecta a una suscripción.
   *
   * @throws PaymentGatewayException si el proveedor falla al responder.
   */
  Optional<VerifiedPaymentEvent> verifyEvent(String eventId);
}
