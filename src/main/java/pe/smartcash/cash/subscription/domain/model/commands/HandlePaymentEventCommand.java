package pe.smartcash.cash.subscription.domain.model.commands;

/**
 * Llegó un webhook del proveedor de pagos. Solo se confía en el {@code eventId}: el resto del
 * cuerpo del webhook se ignora y el evento se vuelve a leer de la API del proveedor con nuestra
 * llave secreta (ver {@code SubscriptionPaymentGateway#verifyEvent}).
 */
public record HandlePaymentEventCommand(String eventId) {}
