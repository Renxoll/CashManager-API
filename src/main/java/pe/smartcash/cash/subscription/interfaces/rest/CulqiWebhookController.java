package pe.smartcash.cash.subscription.interfaces.rest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.smartcash.cash.subscription.domain.services.SubscriptionCommandService;
import pe.smartcash.cash.subscription.interfaces.rest.transform.SubscriptionCommandFromResourceAssembler;

/**
 * Webhooks de Culqi. Público (ver {@code SecurityConfig}): Culqi no manda un Bearer de IAM.
 *
 * <p>Culqi no firma sus webhooks, así que la autenticación es en dos capas:
 * <ol>
 *   <li>{@code ?token=} en la URL registrada en el CulqiPanel (mismo criterio que el webhook de
 *       SendGrid): descarta barato a quien solo adivine la ruta.
 *   <li>Del cuerpo solo se toma el {@code id} del evento; el evento real se vuelve a leer de la
 *       API de Culqi con nuestra llave secreta ({@code SubscriptionPaymentGateway#verifyEvent}).
 *       Un evento inventado no existe en Culqi y se ignora, aunque alguien conozca el token.
 * </ol>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/subscriptions")
class CulqiWebhookController {

  private final SubscriptionCommandService subscriptionCommandService;
  private final byte[] webhookToken;

  CulqiWebhookController(
      SubscriptionCommandService subscriptionCommandService, @Value("${app.culqi.webhook-token}") String webhookToken) {
    this.subscriptionCommandService = subscriptionCommandService;
    this.webhookToken = webhookToken.getBytes(StandardCharsets.UTF_8);
  }

  @PostMapping("/culqi-webhook")
  ResponseEntity<Void> handleWebhook(
      @RequestParam(value = "token", required = false) String token, @RequestBody(required = false) Map<String, Object> body) {
    // Comparación en tiempo constante: un equals() normal filtra cuántos caracteres coinciden.
    if (token == null || !MessageDigest.isEqual(webhookToken, token.getBytes(StandardCharsets.UTF_8))) {
      log.warn("Webhook de Culqi con token inválido");
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    Object eventId = body != null ? body.get("id") : null;
    if (!(eventId instanceof String id) || id.isBlank()) {
      return ResponseEntity.badRequest().build();
    }
    subscriptionCommandService.handle(SubscriptionCommandFromResourceAssembler.toHandlePaymentEventCommand(id));
    return ResponseEntity.ok().build();
  }
}
