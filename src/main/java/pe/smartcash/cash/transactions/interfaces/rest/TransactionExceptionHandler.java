package pe.smartcash.cash.transactions.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.smartcash.cash.shared.interfaces.rest.ApiError;
import pe.smartcash.cash.transactions.domain.exception.UserNotFoundException;

/**
 * Mapeo de excepciones propias de este bounded context; el resto lo cubre el manejador
 * compartido. Spring NO combina por especificidad los @ExceptionHandler de distintos beans
 * @RestControllerAdvice: resuelve con el primer bean (en orden de @Order) que tenga algún
 * método aplicable. Por eso este advice va con precedencia más alta que el catch-all
 * genérico — si no se ordenara así, el catch-all podría interceptar esta excepción primero.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class TransactionExceptionHandler {

  @ExceptionHandler(UserNotFoundException.class)
  ResponseEntity<ApiError> handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiError(Instant.now(), 404, "Not Found", ex.getMessage(), request.getRequestURI()));
  }
}
