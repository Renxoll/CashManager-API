package pe.smartcash.cash.subscription.interfaces.rest;

import java.time.Instant;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.smartcash.cash.shared.interfaces.rest.ApiError;
import pe.smartcash.cash.subscription.domain.exception.ActiveSubscriptionAlreadyExistsException;
import pe.smartcash.cash.subscription.domain.exception.SubscriptionNotFoundException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class SubscriptionExceptionHandler {

  @ExceptionHandler(ActiveSubscriptionAlreadyExistsException.class)
  ResponseEntity<ApiError> handleAlreadyExists(ActiveSubscriptionAlreadyExistsException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(Instant.now(), 409, "Conflict", ex.getMessage()));
  }

  @ExceptionHandler(SubscriptionNotFoundException.class)
  ResponseEntity<ApiError> handleNotFound(SubscriptionNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(Instant.now(), 404, "Not Found", ex.getMessage()));
  }
}
