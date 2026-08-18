package pe.smartcash.cash.profile.interfaces.rest;

import java.time.Instant;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.smartcash.cash.profile.domain.exception.UserProfileAlreadyRegisteredException;
import pe.smartcash.cash.profile.domain.exception.UserProfileNotFoundException;
import pe.smartcash.cash.shared.interfaces.rest.ApiError;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class UserProfileExceptionHandler {

  @ExceptionHandler(UserProfileNotFoundException.class)
  ResponseEntity<ApiError> handleNotFound(UserProfileNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(Instant.now(), 404, "Not Found", ex.getMessage()));
  }

  @ExceptionHandler(UserProfileAlreadyRegisteredException.class)
  ResponseEntity<ApiError> handleAlreadyRegistered(UserProfileAlreadyRegisteredException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(Instant.now(), 409, "Conflict", ex.getMessage()));
  }
}
