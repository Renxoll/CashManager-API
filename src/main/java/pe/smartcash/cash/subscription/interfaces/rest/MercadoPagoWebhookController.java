package pe.smartcash.cash.subscription.interfaces.rest;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.smartcash.cash.subscription.domain.services.SubscriptionCommandService;
import pe.smartcash.cash.subscription.interfaces.rest.transform.SubscriptionCommandFromResourceAssembler;

/**
 * Webhooks de Mercado Pago, URL a registrar en el panel:
 * {@code https://api.tuluki.com/api/v1/subscriptions/mercadopago-webhook}. Público (ver
 * {@code SecurityConfig}); la autenticación es en dos capas:
 * <ol>
 *   <li>La firma {@code x-signature} con la clave secreta del panel
 *       ({@link MercadoPagoWebhookSignature}).
 *   <li>Solo se toman el tipo y el {@code data.id}; el estado real se vuelve a leer del API de
 *       Mercado Pago ({@code SubscriptionPaymentGateway#verifyEvent}).
 * </ol>
 * Sin {@code app.mercadopago.webhook-secret} configurado se rechaza todo.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/subscriptions")
@ConditionalOnProperty(name = "app.payments.provider", havingValue = "mercadopago")
class MercadoPagoWebhookController {

  private final SubscriptionCommandService subscriptionCommandService;
  private final String webhookSecret;

  MercadoPagoWebhookController(
      SubscriptionCommandService subscriptionCommandService, @Value("${app.mercadopago.webhook-secret:}") String webhookSecret) {
    this.subscriptionCommandService = subscriptionCommandService;
    this.webhookSecret = webhookSecret;
  }

  @PostMapping("/mercadopago-webhook")
  ResponseEntity<Void> handleWebhook(
      @RequestParam(value = "data.id", required = false) String dataId,
      @RequestParam(value = "type", required = false) String queryType,
      @RequestHeader(value = "x-signature", required = false) String signature,
      @RequestHeader(value = "x-request-id", required = false) String requestId,
      @RequestBody(required = false) Map<String, Object> body) {
    if (!MercadoPagoWebhookSignature.isValid(webhookSecret, signature, requestId, dataId)) {
      log.warn("Webhook de Mercado Pago con firma inválida");
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    String type = queryType != null ? queryType : body != null && body.get("type") instanceof String t ? t : null;
    if (dataId == null || dataId.isBlank() || type == null || type.isBlank()) {
      return ResponseEntity.badRequest().build();
    }
    subscriptionCommandService.handle(
        SubscriptionCommandFromResourceAssembler.toHandlePaymentEventCommand(type + ":" + dataId));
    return ResponseEntity.ok().build();
  }
}
