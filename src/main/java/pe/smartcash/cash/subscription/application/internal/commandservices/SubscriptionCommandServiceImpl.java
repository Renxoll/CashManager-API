package pe.smartcash.cash.subscription.application.internal.commandservices;

import io.sentry.Sentry;
import java.time.Clock;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;
import pe.smartcash.cash.subscription.domain.exception.ActiveSubscriptionAlreadyExistsException;
import pe.smartcash.cash.subscription.domain.exception.PaymentDeclinedException;
import pe.smartcash.cash.subscription.domain.exception.SubscriptionNotFoundException;
import pe.smartcash.cash.subscription.domain.model.aggregates.Subscription;
import pe.smartcash.cash.subscription.domain.model.aggregates.SubscriptionRepository;
import pe.smartcash.cash.subscription.domain.model.commands.CancelSubscriptionCommand;
import pe.smartcash.cash.subscription.domain.model.commands.HandlePaymentEventCommand;
import pe.smartcash.cash.subscription.domain.model.commands.PayPremiumCommand;
import pe.smartcash.cash.subscription.domain.model.commands.SubscribeCommand;
import pe.smartcash.cash.subscription.domain.model.valueobjects.PlanCode;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionId;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionStatus;
import pe.smartcash.cash.subscription.domain.model.valueobjects.UserId;
import pe.smartcash.cash.subscription.domain.services.PaymentRequest;
import pe.smartcash.cash.subscription.domain.services.PaymentResult;
import pe.smartcash.cash.subscription.domain.services.PremiumPaymentOutcome;
import pe.smartcash.cash.subscription.domain.services.SubscriptionCommandService;
import pe.smartcash.cash.subscription.domain.services.SubscriptionPaymentGateway;
import pe.smartcash.cash.subscription.domain.services.VerifiedPaymentEvent;

@Slf4j
@Service
class SubscriptionCommandServiceImpl implements SubscriptionCommandService {

  private final SubscriptionRepository subscriptionRepository;
  private final SubscriptionPaymentGateway paymentGateway;
  private final TransactionOperations transactions;
  private final Clock clock;

  SubscriptionCommandServiceImpl(
      SubscriptionRepository subscriptionRepository,
      SubscriptionPaymentGateway paymentGateway,
      TransactionOperations transactions,
      Clock clock) {
    this.subscriptionRepository = subscriptionRepository;
    this.paymentGateway = paymentGateway;
    this.transactions = transactions;
    this.clock = clock;
  }

  @Override
  @Transactional
  public SubscriptionId handle(SubscribeCommand command) {
    UserId userId = UserId.of(UUID.fromString(command.userId()));
    if (subscriptionRepository.findActiveByUserId(userId).isPresent()) {
      throw new ActiveSubscriptionAlreadyExistsException(userId);
    }
    Subscription subscription = Subscription.subscribe(SubscriptionId.newId(), userId, PlanCode.fromCode(command.planCode()), clock.instant());
    subscriptionRepository.save(subscription);
    return subscription.id();
  }

  /**
   * Sin {@code @Transactional} a propósito: el cobro son varias llamadas HTTP al proveedor
   * (cliente, tarjeta, suscripción) y no tiene sentido tener una conexión de BD tomada mientras
   * tanto. Solo la activación local va en una transacción corta, al final.
   */
  @Override
  public PremiumPaymentOutcome handle(PayPremiumCommand command) {
    UserId userId = UserId.of(UUID.fromString(command.userId()));
    PlanCode planCode = PlanCode.fromCode(command.planCode());
    if (planCode.term() == null) {
      throw new IllegalArgumentException("El plan " + planCode + " no tiene costo: se activa con SubscribeCommand, sin pago");
    }
    if (!command.acceptedTerms()) {
      // El proveedor exige registrar la aceptación de términos (tyc) al crear la suscripción.
      throw new IllegalArgumentException("Hay que aceptar los términos y condiciones para suscribirse");
    }
    // Un FREE activo no bloquea: es justamente el caso "pasar a Premium". Se cierra recién
    // cuando el cobro se confirma (activatePaid); si el pago falla, el usuario sigue en FREE.
    subscriptionRepository
        .findActiveByUserId(userId)
        .filter(active -> active.planCode() != PlanCode.FREE)
        .ifPresent(active -> {
          throw new ActiveSubscriptionAlreadyExistsException(userId);
        });

    PaymentResult result = paymentGateway.subscribe(toPaymentRequest(userId, planCode, command));
    return switch (result) {
      case PaymentResult.AuthenticationRequired ignored -> PremiumPaymentOutcome.AUTHENTICATION_REQUIRED;
      case PaymentResult.Declined declined -> throw new PaymentDeclinedException(declined.userMessage());
      case PaymentResult.Subscribed subscribed -> {
        activateOrRollBackPayment(userId, planCode, subscribed.providerSubscriptionId());
        yield PremiumPaymentOutcome.ACTIVATED;
      }
    };
  }

  /**
   * El proveedor ya cobró: si la activación local falla (BD caída, carrera con otra pestaña que
   * también pagó), se cancela la suscripción en el proveedor para no seguir cobrándole a alguien
   * que no quedó con Premium. El primer cobro, si ya se hizo, se devuelve a mano desde el panel
   * del proveedor: por eso queda en Sentry.
   */
  private void activateOrRollBackPayment(UserId userId, PlanCode planCode, String providerSubscriptionId) {
    try {
      transactions.executeWithoutResult(status -> activatePaid(userId, planCode, providerSubscriptionId));
    } catch (RuntimeException activationFailure) {
      log.error(
          "Cobro aceptado por el proveedor pero la activación local falló; se cancela la suscripción {} del usuario {}",
          providerSubscriptionId,
          userId.value(),
          activationFailure);
      Sentry.captureException(activationFailure, scope -> scope.setTag("component", "subscription-activation"));
      try {
        paymentGateway.cancel(providerSubscriptionId);
      } catch (RuntimeException cancelFailure) {
        activationFailure.addSuppressed(cancelFailure);
      }
      throw activationFailure;
    }
  }

  /**
   * Punto único de activación de un plan pago. Idempotente por {@code providerSubscriptionId}:
   * activar dos veces el mismo pago no crea una segunda fila.
   */
  private void activatePaid(UserId userId, PlanCode planCode, String providerSubscriptionId) {
    if (subscriptionRepository.findByProviderSubscriptionId(providerSubscriptionId).isPresent()) {
      return;
    }
    var active = subscriptionRepository.findActiveByUserId(userId);
    if (active.isPresent()) {
      Subscription current = active.get();
      if (current.planCode() != PlanCode.FREE) {
        // Otro pago se activó entremedio (dos pestañas pagando a la vez). Se rechaza este para
        // que activateOrRollBackPayment cancele la suscripción duplicada en el proveedor.
        throw new ActiveSubscriptionAlreadyExistsException(userId);
      }
      // Upgrade FREE -> PREMIUM: el FREE se cierra en la misma transacción para respetar
      // "una sola suscripción ACTIVE por usuario" (índice único parcial en la BD).
      current.cancel(clock.instant());
      subscriptionRepository.save(current);
    }
    subscriptionRepository.save(Subscription.subscribe(SubscriptionId.newId(), userId, planCode, clock.instant(), providerSubscriptionId));
  }

  @Override
  @Transactional
  public void handle(CancelSubscriptionCommand command) {
    UserId userId = UserId.of(UUID.fromString(command.userId()));
    Subscription subscription =
        subscriptionRepository.findActiveByUserId(userId).orElseThrow(() -> SubscriptionNotFoundException.noActiveForUser(userId.value()));
    // FREE no tiene providerSubscriptionId: nada que cancelar del lado del proveedor. Se llama
    // al proveedor ANTES de tocar el estado local: si falla, la excepción revierte la transacción
    // y la suscripción queda ACTIVE acá también, consistente con que el proveedor sigue cobrando.
    if (subscription.providerSubscriptionId() != null) {
      paymentGateway.cancel(subscription.providerSubscriptionId());
    }
    subscription.cancel(clock.instant());
    subscriptionRepository.save(subscription);
  }

  /** Igual que el cobro: la verificación contra el proveedor (HTTP) va fuera de la transacción. */
  @Override
  public void handle(HandlePaymentEventCommand command) {
    var verified = paymentGateway.verifyEvent(command.eventId());
    if (verified.isEmpty()) {
      log.info("Webhook del proveedor de pagos ignorado (evento inexistente o sin efecto en suscripciones), eventId={}", command.eventId());
      return;
    }
    transactions.executeWithoutResult(status -> apply(verified.get()));
  }

  private void apply(VerifiedPaymentEvent event) {
    Subscription subscription = subscriptionRepository.findByProviderSubscriptionId(event.providerSubscriptionId()).orElse(null);
    if (subscription == null) {
      // Suscripción que no creamos nosotros (otra integración en la misma cuenta del proveedor)
      // o cuya activación local se revirtió: no hay nada que sincronizar.
      log.warn("Evento {} para una suscripción desconocida del proveedor: {}", event.type(), event.providerSubscriptionId());
      return;
    }
    switch (event.type()) {
      case CHARGE_SUCCEEDED -> renew(subscription);
      case SUBSCRIPTION_CANCELED -> expire(subscription);
      case CHARGE_FAILED -> {
        // El proveedor reintenta el cobro solo y, si se agotan los intentos, cancela la
        // suscripción (llega como SUBSCRIPTION_CANCELED). Acá solo queda visibilidad.
        log.warn("Falló el cobro de la suscripción {} del usuario {}", event.providerSubscriptionId(), subscription.userId().value());
        Sentry.captureMessage(
            "Cobro recurrente fallido (providerSubscriptionId=%s)".formatted(event.providerSubscriptionId()),
            scope -> scope.setTag("component", "payment-webhook"));
      }
    }
  }

  private void renew(Subscription subscription) {
    if (subscription.status() != SubscriptionStatus.ACTIVE) {
      // El webhook llega at-least-once y puede llegar tarde: si ya no está ACTIVE (se canceló
      // entremedio), no hay nada que renovar.
      return;
    }
    subscription.renew(clock.instant());
    subscriptionRepository.save(subscription);
  }

  private void expire(Subscription subscription) {
    if (subscription.status() != SubscriptionStatus.ACTIVE) {
      // Cubre el caso normal de que el usuario la canceló desde la app (lo que la cancela
      // también en el proveedor y dispara este mismo evento después) y los reintentos del webhook.
      return;
    }
    subscription.expire(clock.instant());
    subscriptionRepository.save(subscription);
  }

  private static PaymentRequest toPaymentRequest(UserId userId, PlanCode planCode, PayPremiumCommand command) {
    return new PaymentRequest(
        userId,
        planCode,
        command.cardTokenId(),
        new PaymentRequest.Payer(
            command.firstName(),
            command.lastName(),
            command.email(),
            command.phoneNumber(),
            command.address(),
            command.addressCity(),
            command.countryCode()),
        command.authentication3ds());
  }
}
