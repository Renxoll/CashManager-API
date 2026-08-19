package pe.smartcash.cash.transactions.interfaces.rest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.smartcash.cash.transactions.domain.model.commands.IngestEmailedTransactionCommand;
import pe.smartcash.cash.transactions.domain.services.TransactionCommandService;

/**
 * Ingesta de transacciones por correo (SendGrid Inbound Parse): el usuario reenvía sus
 * notificaciones bancarias a su {@code inboxAddress} (ver {@code UserProfile.register}) y
 * SendGrid las parsea y hace este POST. Solo mapea el {@code multipart/form-data} y delega —
 * la resolución de usuario, la política de remitente confiable y la creación de la
 * transacción viven en {@code TransactionCommandServiceImpl}, no acá.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
class SendGridInboundWebhookController {

  private final TransactionCommandService transactionCommandService;
  private final String inboundToken;

  SendGridInboundWebhookController(
      TransactionCommandService transactionCommandService, @Value("${app.inbound-email.token}") String inboundToken) {
    this.transactionCommandService = transactionCommandService;
    this.inboundToken = inboundToken;
  }

  /**
   * SendGrid Inbound Parse solo permite configurar la URL de destino, no headers custom —
   * por eso el secreto va como query param ({@code ?token=}) en vez de un header como en
   * {@code TransactionAdminController}. Responde 200 siempre que el token sea válido, sin
   * importar si el correo termina descartado (remitente no confiable, buzón desconocido) o
   * si el LLM falla más adelante en el worker async: SendGrid reintenta agresivamente ante
   * cualquier respuesta no-2xx, y ninguno de esos casos se arregla con un reintento.
   */
  @PostMapping(path = "/inbound", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<Void> receiveInboundEmail(
      @RequestParam String token,
      @RequestParam("to") String to,
      @RequestParam("from") String from,
      @RequestParam(value = "subject", required = false) String subject,
      @RequestParam("text") String text) {
    if (!isAuthorized(token)) {
      log.warn("Correo entrante rechazado: token inválido");
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    log.debug("Correo entrante recibido: to={}, from={}, subject={}", to, from, subject);

    transactionCommandService.handle(new IngestEmailedTransactionCommand(to, from, text));
    return ResponseEntity.ok().build();
  }

  private boolean isAuthorized(String providedToken) {
    if (providedToken == null) {
      return false;
    }
    // Comparación en tiempo constante: mismo motivo que en TransactionAdminController.
    return MessageDigest.isEqual(inboundToken.getBytes(StandardCharsets.UTF_8), providedToken.getBytes(StandardCharsets.UTF_8));
  }
}
