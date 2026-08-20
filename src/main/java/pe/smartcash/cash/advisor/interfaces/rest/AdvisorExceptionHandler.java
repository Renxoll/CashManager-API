package pe.smartcash.cash.advisor.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.smartcash.cash.advisor.domain.exception.AdvisorUnavailableException;
import pe.smartcash.cash.shared.interfaces.rest.ApiError;

/**
 * Mapeo de excepciones propias de este bounded context; el resto lo cubre el manejador
 * compartido (ver la nota en {@code GlobalExceptionHandler}). 503, no 500: el request en sí
 * era válido, es el proveedor de LLM el que no respondió -- el cliente puede reintentar.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class AdvisorExceptionHandler {

  @ExceptionHandler(AdvisorUnavailableException.class)
  ResponseEntity<ApiError> handleAdvisorUnavailable(AdvisorUnavailableException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(
            new ApiError(
                Instant.now(),
                503,
                "Service Unavailable",
                "El asesor financiero no está disponible en este momento",
                request.getRequestURI()));
  }
}
