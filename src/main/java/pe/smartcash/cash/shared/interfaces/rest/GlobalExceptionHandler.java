package pe.smartcash.cash.shared.interfaces.rest;

import io.sentry.Sentry;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.smartcash.cash.shared.infrastructure.ratelimiting.RateLimitExceededException;

/**
 * Manejo genérico, transversal a todos los bounded contexts. Excepciones propias de un
 * contexto (p. ej. {@code transactions.domain.exception.UserNotFoundException}) se mapean
 * en un {@code @RestControllerAdvice} local a ese contexto, no acá. Se ordena como último
 * recurso (@Order LOWEST_PRECEDENCE) para que los advices más específicos de cada contexto
 * tengan siempre prioridad — ver la nota en {@code TransactionExceptionHandler}.
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
    String detail =
        ex.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Payload inválido");
    return ResponseEntity.badRequest().body(new ApiError(Instant.now(), 400, "Bad Request", detail, request.getRequestURI()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
    return ResponseEntity.badRequest()
        .body(new ApiError(Instant.now(), 400, "Bad Request", ex.getMessage(), request.getRequestURI()));
  }

  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<ApiError> handleRateLimitExceeded(RateLimitExceededException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.retryAfterSeconds()))
        .body(new ApiError(Instant.now(), 429, "Too Many Requests", ex.getMessage(), request.getRequestURI()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
    log.error("Error no controlado procesando el request", ex);
    // Una vez que este @ExceptionHandler atrapa la excepción, Spring MVC la da por resuelta:
    // la integración automática de Sentry (que escucha HandlerExceptionResolver) nunca la ve.
    // Por eso se captura acá explícitamente en vez de depender del auto-reporting del starter.
    Sentry.captureException(ex, scope -> scope.setTag("component", "unhandled"));
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ApiError(Instant.now(), 500, "Internal Server Error", "Ocurrió un error inesperado", request.getRequestURI()));
  }
}
