package pe.smartcash.cash.gmailsync.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.smartcash.cash.gmailsync.domain.exception.GmailConnectionNotFoundException;
import pe.smartcash.cash.shared.interfaces.rest.ApiError;

/** Mismo criterio que {@code TransactionExceptionHandler}/{@code PendingSenderController} en
 * transactions: precedencia más alta que el catch-all genérico. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class GmailSyncExceptionHandler {

  @ExceptionHandler(GmailConnectionNotFoundException.class)
  ResponseEntity<ApiError> handleGmailConnectionNotFound(GmailConnectionNotFoundException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiError(Instant.now(), 404, "Not Found", ex.getMessage(), request.getRequestURI()));
  }
}
