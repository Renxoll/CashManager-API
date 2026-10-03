package pe.smartcash.cash.subscription.domain.services;

/**
 * Lo que devuelve pagar un plan, para que el controller sepa qué responder. Un rechazo no es
 * un outcome sino una excepción ({@code PaymentDeclinedException}): no hay nada que consultar
 * después.
 */
public enum PremiumPaymentOutcome {
  ACTIVATED,
  AUTHENTICATION_REQUIRED
}
