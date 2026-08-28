package pe.smartcash.cash.gmailsync.domain.exception;

/** El {@code state} del callback no existe (expiró, ya se canjeó, o nunca lo emitió este
 * backend) -- CSRF del flujo OAuth, no un error interno. */
public class InvalidOAuthStateException extends RuntimeException {

  public InvalidOAuthStateException() {
    super("El enlace de conexión con Gmail expiró o ya fue usado. Intenta conectar de nuevo.");
  }
}
