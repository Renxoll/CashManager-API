package pe.smartcash.cash.subscription.interfaces.rest.resources;

/**
 * Respuesta {@code 202} de {@code POST /premium} cuando el banco pide autenticar al titular:
 * {@code status=AUTHENTICATION_REQUIRED}. El frontend abre Culqi3DS y reenvía el mismo pedido,
 * con el mismo {@code cardToken}, más los parámetros 3DS.
 */
public record PremiumPaymentResource(String status) {}
