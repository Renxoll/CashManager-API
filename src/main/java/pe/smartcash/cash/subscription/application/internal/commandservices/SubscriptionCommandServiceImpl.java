package pe.smartcash.cash.subscription.application.internal.commandservices;

import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smartcash.cash.subscription.domain.exception.ActiveSubscriptionAlreadyExistsException;
import pe.smartcash.cash.subscription.domain.exception.SubscriptionNotFoundException;
import pe.smartcash.cash.subscription.domain.model.aggregates.Subscription;
import pe.smartcash.cash.subscription.domain.model.aggregates.SubscriptionRepository;
import pe.smartcash.cash.subscription.domain.model.commands.CancelSubscriptionCommand;
import pe.smartcash.cash.subscription.domain.model.commands.SubscribeCommand;
import pe.smartcash.cash.subscription.domain.model.valueobjects.PlanCode;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionId;
import pe.smartcash.cash.subscription.domain.model.valueobjects.UserId;
import pe.smartcash.cash.subscription.domain.services.SubscriptionCommandService;

@Service
class SubscriptionCommandServiceImpl implements SubscriptionCommandService {

  private final SubscriptionRepository subscriptionRepository;
  private final Clock clock;

  SubscriptionCommandServiceImpl(SubscriptionRepository subscriptionRepository, Clock clock) {
    this.subscriptionRepository = subscriptionRepository;
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
  @Transactional
  public void handle(CancelSubscriptionCommand command) {
    UserId userId = UserId.of(UUID.fromString(command.userId()));
    Subscription subscription =
        subscriptionRepository.findActiveByUserId(userId).orElseThrow(() -> SubscriptionNotFoundException.noActiveForUser(userId.value()));
    subscription.cancel(clock.instant());
    subscriptionRepository.save(subscription);
  }
}
