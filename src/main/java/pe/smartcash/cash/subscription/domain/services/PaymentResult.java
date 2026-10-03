package pe.smartcash.cash.subscription.domain.services;

/** Resultado de intentar suscribir una tarjeta a un plan en el proveedor de pagos. */
public sealed interface PaymentResult {

  /**
   * La suscripción quedó creada en el proveedor con una tarjeta ya validada. Ojo: Culqi cobra
   * las suscripciones en un proceso batch diario, así que el cobro en sí puede llegar horas
   * después; si falla hasta agotar los reintentos, Culqi la cancela y llega por webhook.
   */
  record Subscribed(String providerSubscriptionId) implements PaymentResult {}

  /** El banco exige autenticar al titular (3DS) antes de aceptar la tarjeta. No se cobró nada. */
  record AuthenticationRequired() implements PaymentResult {}

  /**
   * El proveedor rechazó la tarjeta o el cobro (fondos insuficientes, tarjeta robada, etc).
   * {@code userMessage} es el texto que el proveedor redacta para mostrarle al titular.
   */
  record Declined(String userMessage) implements PaymentResult {}
}
