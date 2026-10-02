package pe.smartcash.cash.subscription.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.smartcash.cash.shared.interfaces.rest.ApiError;
import pe.smartcash.cash.subscription.domain.exception.ActiveSubscriptionAlreadyExistsException;
import pe.smartcash.cash.subscription.domain.exception.PaymentDeclinedException;
import pe.smartcash.cash.subscription.domain.exception.PaymentGatewayException;
import pe.smartcash.cash.subscription.domain.exception.SubscriptionNotFoundException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class SubscriptionExceptionHandler {

  @ExceptionHandler(ActiveSubscriptionAlreadyExistsException.class)
  ResponseEntity<ApiError> handleAlreadyExists(ActiveSubscriptionAlreadyExistsException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ApiError(Instant.now(), 409, "Conflict", ex.getMessage(), request.getRequestURI()));
  }

  @ExceptionHandler(SubscriptionNotFoundException.class)
  ResponseEntity<ApiError> handleNotFound(SubscriptionNotFoundException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiError(Instant.now(), 404, "Not Found", ex.getMessage(), request.getRequestURI()));
  }

  // 402 Payment Required: la tarjeta fue rechazada. El mensaje es el que Culqi redacta para el
  // titular (p. ej. "Tu tarjeta no tiene fondos suficientes"), así que el frontend lo muestra tal cual.
  @ExceptionHandler(PaymentDeclinedException.class)
  ResponseEntity<ApiError> handlePaymentDeclined(PaymentDeclinedException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
        .body(new ApiError(Instant.now(), 402, "Payment Required", ex.getMessage(), request.getRequestURI()));
  }

  @ExceptionHandler(PaymentGatewayException.class)
  ResponseEntity<ApiError> handlePaymentGatewayFailure(PaymentGatewayException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(new ApiError(Instant.now(), 502, "Bad Gateway", ex.getMessage(), request.getRequestURI()));
  }
}
