package pe.smartcash.cash.iam.domain.exception;

/** Deliberadamente no dice si falló el email o la contraseña: no le da pistas a un atacante. */
public class InvalidCredentialsException extends RuntimeException {

  public InvalidCredentialsException() {
    super("Email o contraseña inválidos");
  }
}
