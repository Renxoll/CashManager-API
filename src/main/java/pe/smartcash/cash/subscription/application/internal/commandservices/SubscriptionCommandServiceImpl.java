package pe.smartcash.cash.subscription.application.internal.commandservices;

import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smartcash.cash.subscription.domain.exception.ActiveSubscriptionAlreadyExistsException;
import pe.smartcash.cash.subscription.domain.exception.SubscriptionNotFoundException;
import pe.smartcash.cash.subscription.domain.model.aggregates.Subscription;
import pe.smartcash.cash.subscription.domain.model.aggregates.SubscriptionRepository;
import pe.smartcash.cash.subscription.domain.model.commands.ActivateSubscriptionCommand;
import pe.smartcash.cash.subscription.domain.model.commands.CancelSubscriptionCommand;
import pe.smartcash.cash.subscription.domain.model.commands.StartCheckoutCommand;
import pe.smartcash.cash.subscription.domain.model.commands.SubscribeCommand;
import pe.smartcash.cash.subscription.domain.model.valueobjects.PlanCode;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionId;
import pe.smartcash.cash.subscription.domain.model.valueobjects.UserId;
import pe.smartcash.cash.subscription.domain.services.CheckoutSession;
import pe.smartcash.cash.subscription.domain.services.SubscriptionCommandService;
import pe.smartcash.cash.subscription.domain.services.SubscriptionPaymentGateway;

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
    Subscription subscription = Subscription.subscribe(SubscriptionId.newId(), userId, PlanCode.fromCode(command.planCode()), clock.instant());
    subscriptionRepository.save(subscription);
  }

  @Override
  @Transactional
  public void handle(CancelSubscriptionCommand command) {
    UserId userId = UserId.of(UUID.fromString(command.userId()));
    Subscription subscription =
        subscriptionRepository.findActiveByUserId(userId).orElseThrow(() -> SubscriptionNotFoundException.noActiveForUser(userId.value()));
    subscription.cancel(clock.instant());
    subscriptionRepository.save(subscription);
  }
}
