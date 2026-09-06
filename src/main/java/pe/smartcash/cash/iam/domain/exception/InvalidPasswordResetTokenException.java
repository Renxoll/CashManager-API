package pe.smartcash.cash.iam.domain.exception;

/**
 * El token de restablecimiento recibido en el confirm no existe, ya se usó o expiró. Un
 * único caso para los tres: no se le dice al cliente cuál de ellos fue.
 */
public class InvalidPasswordResetTokenException extends RuntimeException {

  public InvalidPasswordResetTokenException() {
    super("El enlace de restablecimiento no es válido o expiró");
  }
}
