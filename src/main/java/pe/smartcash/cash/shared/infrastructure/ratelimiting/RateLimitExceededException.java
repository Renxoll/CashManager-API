package pe.smartcash.cash.shared.infrastructure.ratelimiting;

/**
 * Señal de infraestructura (cupo de Bucket4j agotado), no un error de negocio: cruza de
 * infrastructure a interfaces (ver el {@code @ExceptionHandler} en {@code
 * GlobalExceptionHandler}) sin pasar por ningún puerto de dominio, a propósito -- rate
 * limiting no es una regla que ningún bounded context deba conocer o proteger.
 */
public class RateLimitExceededException extends RuntimeException {

  private final long retryAfterSeconds;

  public RateLimitExceededException(String key, long retryAfterSeconds) {
    super("Límite de peticiones excedido para: " + key);
    this.retryAfterSeconds = retryAfterSeconds;
  }

  public long retryAfterSeconds() {
    return retryAfterSeconds;
  }
}
