package pe.smartcash.cash.subscription.domain.model.commands;

/**
 * El usuario paga un plan con la tarjeta que tokenizó el checkout del proveedor de pagos en el
 * navegador ({@code cardTokenId}: los datos de la tarjeta nunca pasan por este backend). Los
 * datos del pagador los exige el proveedor para crear el cliente y alimentar su antifraude; no
 * se guardan en nuestra BD.
 *
 * <p>{@code authentication3ds} viaja vacío en el primer intento. Si el banco pide autenticar
 * (3DS), el frontend la resuelve y reenvía este mismo comando, con el mismo token, ahora con
 * esos parámetros.
 */
public record PayPremiumCommand(
    String userId,
    String planCode,
    String cardTokenId,
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    String address,
    String addressCity,
    String countryCode,
    boolean acceptedTerms,
    ThreeDSecureParameters authentication3ds) {

  /** Resultado de la autenticación 3DS tal como lo devuelve la librería del proveedor en el navegador. */
  public record ThreeDSecureParameters(String eci, String xid, String cavv, String protocolVersion, String directoryServerTransactionId) {}
}
