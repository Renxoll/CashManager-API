package pe.smartcash.cash.subscription.domain.services;

/**
 * Un evento de webhook ya confirmado contra la API del proveedor de pagos. Solo llegan acá los
 * tipos que le importan al ciclo de vida de una suscripción; el resto se descarta en el adaptador.
 */
public record VerifiedPaymentEvent(String eventId, Type type, String providerSubscriptionId) {

  public enum Type {
    /** Se cobró el período siguiente de la suscripción. */
    CHARGE_SUCCEEDED,
    /** Falló un cobro; el proveedor reintenta solo hasta su límite y después cancela. */
    CHARGE_FAILED,
    /** La suscripción terminó del lado del proveedor (reintentos agotados o baja desde su panel). */
    SUBSCRIPTION_CANCELED
  }
}
