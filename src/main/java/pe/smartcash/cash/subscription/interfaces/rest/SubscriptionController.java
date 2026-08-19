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
import pe.smartcash.cash.subscription.domain.services.SubscriptionCommandService;
import pe.smartcash.cash.subscription.domain.services.SubscriptionQueryService;
import pe.smartcash.cash.subscription.interfaces.rest.resources.CheckoutSessionResource;
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

  /**
   * FREE no pasa por Stripe (no hay nada que cobrar): se activa igual que antes y devuelve
   * 201 con el recurso ya creado. PREMIUM nunca se activa acá — devuelve 200 con la URL de
   * Stripe Checkout; la activación real la dispara {@link StripeWebhookController} recién
   * cuando Stripe confirma el pago.
   */
  @PostMapping("/checkout")
  ResponseEntity<?> checkout(@AuthenticationPrincipal String authenticatedUserId, @Valid @RequestBody SubscribeResource resource) {
    if (PlanCode.fromCode(resource.planCode()) == PlanCode.FREE) {
      subscriptionCommandService.handle(SubscriptionCommandFromResourceAssembler.toSubscribeCommand(authenticatedUserId, resource));
      return ResponseEntity.status(HttpStatus.CREATED).body(fetch(UserId.of(UUID.fromString(authenticatedUserId))));
    }

    var checkoutSession =
        subscriptionCommandService.handle(SubscriptionCommandFromResourceAssembler.toStartCheckoutCommand(authenticatedUserId, resource));
    return ResponseEntity.ok(new CheckoutSessionResource(checkoutSession.checkoutUrl()));
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
