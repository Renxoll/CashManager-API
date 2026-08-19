package pe.smartcash.cash.iam.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.smartcash.cash.iam.domain.exception.EmailAlreadyRegisteredException;
import pe.smartcash.cash.iam.domain.exception.InvalidCredentialsException;
import pe.smartcash.cash.shared.interfaces.rest.ApiError;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class IamExceptionHandler {

  @ExceptionHandler(EmailAlreadyRegisteredException.class)
  ResponseEntity<ApiError> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ApiError(Instant.now(), 409, "Conflict", ex.getMessage(), request.getRequestURI()));
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new ApiError(Instant.now(), 401, "Unauthorized", ex.getMessage(), request.getRequestURI()));
  }
}
