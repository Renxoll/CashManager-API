package pe.smartcash.cash.subscription.interfaces.rest;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.smartcash.cash.subscription.domain.services.SubscriptionCommandService;
import pe.smartcash.cash.subscription.interfaces.rest.transform.SubscriptionCommandFromResourceAssembler;

/**
 * Público (ver {@code SecurityConfig}): Stripe no manda un Bearer token de IAM, así que
 * exigirlo lo bloquearía a las puertas. La verificación de la firma HMAC contra {@code
 * STRIPE_WEBHOOK_SECRET} es la autenticación real de este endpoint — es la única forma de
 * confirmar que el request efectivamente viene de Stripe y no de cualquiera que adivine la
 * URL. Por eso el body se recibe como {@code String} crudo, no como un DTO deserializado: la
 * firma se calcula sobre los bytes exactos que Stripe envió, y pasar por Jackson primero
 * (reordenando o normalizando espacios) la invalidaría.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/subscriptions")
class StripeWebhookController {

  private static final String EVENT_CHECKOUT_SESSION_COMPLETED = "checkout.session.completed";
  private static final String EVENT_INVOICE_PAID = "invoice.paid";
  private static final String EVENT_INVOICE_PAYMENT_FAILED = "invoice.payment_failed";
  private static final String EVENT_CUSTOMER_SUBSCRIPTION_DELETED = "customer.subscription.deleted";

  private final SubscriptionCommandService subscriptionCommandService;
  private final String webhookSecret;

  StripeWebhookController(
      SubscriptionCommandService subscriptionCommandService, @Value("${stripe.webhook-secret}") String webhookSecret) {
    this.subscriptionCommandService = subscriptionCommandService;
    this.webhookSecret = webhookSecret;
  }

  @PostMapping("/stripe-webhook")
  ResponseEntity<Void> handleWebhook(
      @RequestBody String payload, @RequestHeader(value = "Stripe-Signature", required = false) String signatureHeader) {
    // required=false + chequeo manual, en vez de dejar que Spring MVC lo exija: el catch-all
    // de GlobalExceptionHandler intercepta MissingRequestHeaderException antes que el manejo
    // por defecto de Spring (que la mapea a 400), y termina devolviendo un 500 genérico.
    if (signatureHeader == null) {
      return ResponseEntity.badRequest().build();
    }
    Event event;
    try {
      event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
    } catch (SignatureVerificationException invalidSignature) {
      log.warn("Firma de webhook de Stripe inválida: {}", invalidSignature.getMessage());
      // No se relanza (Stripe reintentaría un webhook que nunca va a validar), así que sin
      // esta captura explícita el fallo jamás llegaría a Sentry.
      Sentry.captureException(invalidSignature, scope -> scope.setTag("component", "stripe-webhook"));
      return ResponseEntity.badRequest().build();
    }

    switch (event.getType()) {
      case EVENT_CHECKOUT_SESSION_COMPLETED -> activateFromCompletedCheckout(event);
      case EVENT_INVOICE_PAID -> renewFromPaidInvoice(event);
      case EVENT_INVOICE_PAYMENT_FAILED -> alertFailedInvoicePayment(event);
      case EVENT_CUSTOMER_SUBSCRIPTION_DELETED -> expireFromDeletedSubscription(event);
      // 200 para cualquier otro tipo de evento que no nos interesa: confirma la recepción sin
      // procesarlo, así Stripe no lo reintenta pensando que falló.
      default -> log.debug("Evento de Stripe ignorado: {}", event.getType());
    }
    return ResponseEntity.ok().build();
  }

  private void activateFromCompletedCheckout(Event event) {
    StripeObject stripeObject =
        event.getDataObjectDeserializer().getObject().orElseThrow(() -> new IllegalStateException("Evento sin payload deserializable"));
    Session session = (Session) stripeObject;

    String userId = session.getMetadata().get("userId");
    String planCode = session.getMetadata().get("planCode");
    if (userId == null || planCode == null) {
      // No debería pasar nunca: userId/planCode se setean como metadata al crear la sesión
      // en StripePaymentGatewayAdapter. Si llega sin ellos, es un evento que no originamos
      // nosotros (o Stripe cambió el shape) -- se ignora en vez de reventar el webhook.
      log.error("checkout.session.completed sin metadata userId/planCode, sessionId={}", session.getId());
      return;
    }
    // El id de la Subscription que Stripe crea junto con el pago (distinto del id de la
    // Session) -- se necesita guardado para poder cancelarla después vía la API de Stripe.
    String stripeSubscriptionId = session.getSubscription();

    subscriptionCommandService.handle(
        SubscriptionCommandFromResourceAssembler.toActivateSubscriptionCommand(userId, planCode, stripeSubscriptionId));
  }

  /**
   * Stripe cobró automáticamente el ciclo siguiente de una suscripción en modo "subscription".
   * Desde la reestructuración 2025 de invoicing, el id de la Subscription ya no viaja en
   * {@code invoice.subscription} (ese campo no existe más) sino en
   * {@code invoice.parent.subscription_details.subscription}.
   */
  private void renewFromPaidInvoice(Event event) {
    StripeObject stripeObject =
        event.getDataObjectDeserializer().getObject().orElseThrow(() -> new IllegalStateException("Evento sin payload deserializable"));
    Invoice invoice = (Invoice) stripeObject;

    String stripeSubscriptionId = subscriptionIdOf(invoice);
    if (stripeSubscriptionId == null) {
      // Factura que no corresponde a ninguna suscripción recurrente (p. ej. un cargo único)
      // -- no hay nada que renovar acá.
      return;
    }
    subscriptionCommandService.handle(SubscriptionCommandFromResourceAssembler.toRenewCommand(stripeSubscriptionId));
  }

  /**
   * Un cobro de renovación falló (tarjeta rechazada, fondos insuficientes, etc). Stripe ya
   * tiene su propio calendario de reintentos (Smart Retries) y termina resolviendo esto solo
   * -- reintenta y dispara {@code invoice.paid}, o agota los reintentos y dispara {@code
   * customer.subscription.deleted} -- así que acá no se muta nada todavía, solo se alerta para
   * que quede visibilidad de que un cliente tiene un cobro en problemas.
   */
  private void alertFailedInvoicePayment(Event event) {
    StripeObject stripeObject =
        event.getDataObjectDeserializer().getObject().orElseThrow(() -> new IllegalStateException("Evento sin payload deserializable"));
    Invoice invoice = (Invoice) stripeObject;

    String stripeSubscriptionId = subscriptionIdOf(invoice);
    log.warn(
        "Falló el cobro de una factura de Stripe, invoiceId={}, stripeSubscriptionId={}, intento={}",
        invoice.getId(),
        stripeSubscriptionId,
        invoice.getAttemptCount());
    Sentry.captureMessage(
        "Stripe invoice.payment_failed (invoiceId=%s, stripeSubscriptionId=%s)".formatted(invoice.getId(), stripeSubscriptionId),
        scope -> scope.setTag("component", "stripe-webhook"));
  }

  /**
   * La suscripción recurrente terminó del lado de Stripe: reintentos de cobro agotados, o se
   * canceló directo desde el dashboard de Stripe (no desde la app -- eso ya lo cubre {@code
   * SubscriptionController.cancelActive}, que llama a Stripe y este evento llega después como
   * confirmación redundante).
   */
  private void expireFromDeletedSubscription(Event event) {
    StripeObject stripeObject =
        event.getDataObjectDeserializer().getObject().orElseThrow(() -> new IllegalStateException("Evento sin payload deserializable"));
    Subscription subscription = (Subscription) stripeObject;

    subscriptionCommandService.handle(SubscriptionCommandFromResourceAssembler.toExpireCommand(subscription.getId()));
  }

  private static String subscriptionIdOf(Invoice invoice) {
    if (invoice.getParent() == null || invoice.getParent().getSubscriptionDetails() == null) {
      return null;
    }
    return invoice.getParent().getSubscriptionDetails().getSubscription();
  }
}
