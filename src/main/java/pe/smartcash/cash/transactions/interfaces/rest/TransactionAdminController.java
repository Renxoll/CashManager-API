package pe.smartcash.cash.transactions.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.smartcash.cash.shared.interfaces.rest.ApiError;
import pe.smartcash.cash.transactions.domain.model.commands.RetryFailedTransactionsCommand;
import pe.smartcash.cash.transactions.domain.services.RetryFailedTransactionsResult;
import pe.smartcash.cash.transactions.domain.services.TransactionCommandService;
import pe.smartcash.cash.transactions.interfaces.rest.resources.RetryFailedTransactionsResource;

/**
 * Endpoints operativos, no de negocio: pensados para un cron/proceso interno, no para un
 * usuario final de la app. Además del Bearer token normal (ya lo exige {@code SecurityConfig}
 * para cualquier ruta no listada en {@code permitAll()}), exige un segundo secreto compartido
 * por header — dos factores de "quién puede llamar esto" en vez de uno, sin tener que montar
 * todavía un sistema de roles/RBAC completo que ningún otro endpoint de la API necesita hoy.
 */
@RestController
@RequestMapping("/api/v1/transactions")
class TransactionAdminController {

  private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

  private final TransactionCommandService transactionCommandService;
  private final String internalRetryToken;

  TransactionAdminController(
      TransactionCommandService transactionCommandService, @Value("${app.internal.retry-token}") String internalRetryToken) {
    this.transactionCommandService = transactionCommandService;
    this.internalRetryToken = internalRetryToken;
  }

  @PostMapping("/retry-failed")
  ResponseEntity<?> retryFailed(
      @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String providedToken, HttpServletRequest request) {
    if (!isAuthorized(providedToken)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(
              new ApiError(
                  Instant.now(),
                  403,
                  "Forbidden",
                  "Este endpoint requiere el header " + INTERNAL_TOKEN_HEADER,
                  request.getRequestURI()));
    }

    RetryFailedTransactionsResult result = transactionCommandService.handle(new RetryFailedTransactionsCommand());
    return ResponseEntity.ok(new RetryFailedTransactionsResource(result.retried(), result.stillFailed()));
  }

  private boolean isAuthorized(String providedToken) {
    if (providedToken == null) {
      return false;
    }
    // Comparación en tiempo constante: mismo motivo que la firma HMAC de los access tokens
    // (ver HmacTokenServiceAdapter) -- no delatar, vía cuánto tarda la comparación, en qué
    // caracter difiere el token recibido del real.
    return MessageDigest.isEqual(
        internalRetryToken.getBytes(StandardCharsets.UTF_8), providedToken.getBytes(StandardCharsets.UTF_8));
  }
}
