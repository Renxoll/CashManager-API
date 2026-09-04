package pe.smartcash.cash.subscription.application.internal.commandservices;

import java.time.Clock;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smartcash.cash.subscription.domain.exception.ActiveSubscriptionAlreadyExistsException;
import pe.smartcash.cash.subscription.domain.exception.SubscriptionNotFoundException;
import pe.smartcash.cash.subscription.domain.model.aggregates.Subscription;
import pe.smartcash.cash.subscription.domain.model.aggregates.SubscriptionRepository;
import pe.smartcash.cash.subscription.domain.model.commands.ActivateSubscriptionCommand;
import pe.smartcash.cash.subscription.domain.model.commands.CancelSubscriptionCommand;
import pe.smartcash.cash.subscription.domain.model.commands.ExpireSubscriptionCommand;
import pe.smartcash.cash.subscription.domain.model.commands.RenewSubscriptionCommand;
import pe.smartcash.cash.subscription.domain.model.commands.StartCheckoutCommand;
import pe.smartcash.cash.subscription.domain.model.commands.SubscribeCommand;
import pe.smartcash.cash.subscription.domain.model.valueobjects.PlanCode;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionId;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionStatus;
import pe.smartcash.cash.subscription.domain.model.valueobjects.UserId;
import pe.smartcash.cash.subscription.domain.services.CheckoutSession;
import pe.smartcash.cash.subscription.domain.services.SubscriptionCommandService;
import pe.smartcash.cash.subscription.domain.services.SubscriptionPaymentGateway;

@Slf4j
@Service
class SubscriptionCommandServiceImpl implements SubscriptionCommandService {

  private final SubscriptionRepository subscriptionRepository;
  private final SubscriptionPaymentGateway paymentGateway;
  private final Clock clock;

  SubscriptionCommandServiceImpl(
      SubscriptionRepository subscriptionRepository, SubscriptionPaymentGateway paymentGateway, Clock clock) {
    this.subscriptionRepository = subscriptionRepository;
    this.paymentGateway = paymentGateway;
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

  @Override
  public CheckoutSession handle(StartCheckoutCommand command) {
    UserId userId = UserId.of(UUID.fromString(command.userId()));
    if (subscriptionRepository.findActiveByUserId(userId).isPresent()) {
      throw new ActiveSubscriptionAlreadyExistsException(userId);
    }
    // No @Transactional: no toca la BD, solo llama al gateway externo. Nada que activar
    // todavía -- eso pasa recién cuando llegue el webhook con el pago confirmado.
    return paymentGateway.startCheckout(userId, PlanCode.fromCode(command.planCode()));
  }

  @Override
  @Transactional
  public void handle(ActivateSubscriptionCommand command) {
    UserId userId = UserId.of(UUID.fromString(command.userId()));
    if (subscriptionRepository.findActiveByUserId(userId).isPresent()) {
      // Stripe entrega el mismo evento más de una vez por diseño (at-least-once delivery);
      // el webhook debe ser idempotente. Si ya está ACTIVE, este evento ya se procesó antes
      // (o el usuario ya tenía otra suscripción activa) -- no es un error, es un no-op.
      return;
    }
    Subscription subscription =
        Subscription.subscribe(
            SubscriptionId.newId(), userId, PlanCode.fromCode(command.planCode()), clock.instant(), command.stripeSubscriptionId());
    subscriptionRepository.save(subscription);
  }

  @Override
  @Transactional
  public void handle(CancelSubscriptionCommand command) {
    UserId userId = UserId.of(UUID.fromString(command.userId()));
    Subscription subscription =
        subscriptionRepository.findActiveByUserId(userId).orElseThrow(() -> SubscriptionNotFoundException.noActiveForUser(userId.value()));
    // FREE (y cualquier suscripción activada antes de que este id se empezara a guardar) no
    // tiene stripeSubscriptionId -- nada que cancelar del lado del proveedor de pagos. Se
    // llama a Stripe ANTES de tocar el estado local: si el gateway falla, la excepción revierte
    // la transacción y la suscripción queda ACTIVE acá también, consistente con que en Stripe
    // sigue cobrando -- evita el escenario contrario (CANCELED acá, pero Stripe sigue
    // facturando porque la llamada nunca se hizo).
    if (subscription.stripeSubscriptionId() != null) {
      paymentGateway.cancel(subscription.stripeSubscriptionId());
    }
    subscription.cancel(clock.instant());
    subscriptionRepository.save(subscription);
  }

  @Override
  @Transactional
  public void handle(RenewSubscriptionCommand command) {
    Subscription subscription = subscriptionRepository.findByStripeSubscriptionId(command.stripeSubscriptionId()).orElse(null);
    if (subscription == null) {
      // No debería pasar nunca: invoice.paid solo llega para suscripciones que nosotros
      // creamos vía Checkout. Si el id no está en nuestra BD, es un evento que no originamos
      // (o el id se perdió antes de que este campo empezara a guardarse) -- se ignora en vez
      // de reventar el webhook.
      log.error("invoice.paid para una Subscription de Stripe desconocida, stripeSubscriptionId={}", command.stripeSubscriptionId());
      return;
    }
    if (subscription.status() != SubscriptionStatus.ACTIVE) {
      // Idempotencia ante reintentos de Stripe, igual que ActivateSubscriptionCommand: si ya
      // no está ACTIVE (p. ej. se canceló entremedio), no hay nada que renovar.
      return;
    }
    subscription.renew(clock.instant());
    subscriptionRepository.save(subscription);
  }

  @Override
  @Transactional
  public void handle(ExpireSubscriptionCommand command) {
    Subscription subscription = subscriptionRepository.findByStripeSubscriptionId(command.stripeSubscriptionId()).orElse(null);
    if (subscription == null) {
      log.error(
          "customer.subscription.deleted para una Subscription de Stripe desconocida, stripeSubscriptionId={}",
          command.stripeSubscriptionId());
      return;
    }
    if (subscription.status() != SubscriptionStatus.ACTIVE) {
      // Cubre dos casos idempotentes: el usuario ya la había cancelado desde la app (lo que
      // también la cancela en Stripe y dispara este mismo evento después), o Stripe reintentó
      // la entrega del webhook.
      return;
    }
    subscription.expire(clock.instant());
    subscriptionRepository.save(subscription);
  }
}
