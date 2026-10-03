package pe.smartcash.cash.subscription.interfaces.rest;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.smartcash.cash.subscription.domain.exception.SubscriptionNotFoundException;
import pe.smartcash.cash.subscription.domain.model.queries.FindActiveSubscriptionByUserIdQuery;
import pe.smartcash.cash.subscription.domain.model.valueobjects.PlanCode;
import pe.smartcash.cash.subscription.domain.model.valueobjects.UserId;
import pe.smartcash.cash.subscription.domain.services.PremiumPaymentOutcome;
import pe.smartcash.cash.subscription.domain.services.SubscriptionCommandService;
import pe.smartcash.cash.subscription.domain.services.SubscriptionQueryService;
import pe.smartcash.cash.subscription.interfaces.rest.resources.PayPremiumResource;
import pe.smartcash.cash.subscription.interfaces.rest.resources.PremiumPaymentResource;
import pe.smartcash.cash.subscription.interfaces.rest.resources.SubscribeResource;
import pe.smartcash.cash.subscription.interfaces.rest.resources.SubscriptionResource;
import pe.smartcash.cash.subscription.interfaces.rest.transform.SubscriptionCommandFromResourceAssembler;
import pe.smartcash.cash.subscription.interfaces.rest.transform.SubscriptionResourceFromEntityAssembler;

/**
 * El request ya pasó por {@code BearerTokenAuthenticationFilter} de IAM antes de llegar
 * acá: el {@code userId} se toma del principal ya autenticado, nunca del body ni de un path
 * variable — no hay "suscripción de otro usuario" que un cliente pueda referenciar.
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
class SubscriptionController {

  private final SubscriptionCommandService subscriptionCommandService;
  private final SubscriptionQueryService subscriptionQueryService;

  SubscriptionController(SubscriptionCommandService subscriptionCommandService, SubscriptionQueryService subscriptionQueryService) {
    this.subscriptionCommandService = subscriptionCommandService;
    this.subscriptionQueryService = subscriptionQueryService;
  }

  /** Alta de un plan sin costo (FREE): no pasa por el proveedor de pagos. Los planes pagos van por {@code /premium}. */
  @PostMapping("/checkout")
  ResponseEntity<SubscriptionResource> checkout(
      @AuthenticationPrincipal String authenticatedUserId, @Valid @RequestBody SubscribeResource resource) {
    if (PlanCode.fromCode(resource.planCode()).term() != null) {
      throw new IllegalArgumentException("El plan " + resource.planCode() + " es pago: usa POST /api/v1/subscriptions/premium");
    }
    subscriptionCommandService.handle(SubscriptionCommandFromResourceAssembler.toSubscribeCommand(authenticatedUserId, resource));
    return ResponseEntity.status(HttpStatus.CREATED).body(fetch(UserId.of(UUID.fromString(authenticatedUserId))));
  }

  /**
   * Cobra el plan con la tarjeta que tokenizó el checkout de Culqi. {@code 201} con la
   * suscripción ya activa; {@code 202} si el banco pide 3DS (el frontend autentica y reenvía);
   * {@code 402} si la tarjeta es rechazada; {@code 409} si ya tiene un plan pago; {@code 502}
   * si Culqi falla.
   */
  @PostMapping("/premium")
  ResponseEntity<?> payPremium(@AuthenticationPrincipal String authenticatedUserId, @Valid @RequestBody PayPremiumResource resource) {
    PremiumPaymentOutcome outcome =
        subscriptionCommandService.handle(SubscriptionCommandFromResourceAssembler.toPayPremiumCommand(authenticatedUserId, resource));
    if (outcome == PremiumPaymentOutcome.AUTHENTICATION_REQUIRED) {
      return ResponseEntity.status(HttpStatus.ACCEPTED).body(new PremiumPaymentResource(outcome.name()));
    }
    return ResponseEntity.status(HttpStatus.CREATED).body(fetch(UserId.of(UUID.fromString(authenticatedUserId))));
  }

  @DeleteMapping("/active")
  ResponseEntity<Void> cancelActive(@AuthenticationPrincipal String authenticatedUserId) {
    subscriptionCommandService.handle(SubscriptionCommandFromResourceAssembler.toCancelCommand(authenticatedUserId));
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/active")
  ResponseEntity<SubscriptionResource> getActive(@AuthenticationPrincipal String authenticatedUserId) {
    return ResponseEntity.ok(fetch(UserId.of(UUID.fromString(authenticatedUserId))));
  }

  private SubscriptionResource fetch(UserId userId) {
    var detail =
        subscriptionQueryService
            .handle(new FindActiveSubscriptionByUserIdQuery(userId))
            .orElseThrow(() -> SubscriptionNotFoundException.noActiveForUser(userId.value()));
    return SubscriptionResourceFromEntityAssembler.toResourceFromEntity(detail);
  }
}
