package pe.smartcash.cash.iam.domain.services;

/**
 * Puerto para acuñar el token de restablecimiento y para reconstruir su hash. El token
 * crudo solo viaja en el enlace del correo; lo único que se persiste es {@link #hash}
 * del mismo, y el confirm vuelve a hashear lo que recibe para buscar la fila.
 */
public interface PasswordResetTokenGenerator {

  GeneratedToken generate();

  /** Hash determinístico del token crudo recibido en el confirm, para el lookup por hash. */
  String hash(String rawToken);

  /** {@code rawToken} va en el enlace del correo; {@code tokenHash} es lo que se guarda. */
  record GeneratedToken(String rawToken, String tokenHash) {}
}
