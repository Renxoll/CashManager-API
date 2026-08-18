package pe.smartcash.cash.subscription.application.internal.queryservices;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smartcash.cash.subscription.domain.model.aggregates.Subscription;
import pe.smartcash.cash.subscription.domain.model.aggregates.SubscriptionRepository;
import pe.smartcash.cash.subscription.domain.model.queries.FindActiveSubscriptionByUserIdQuery;
import pe.smartcash.cash.subscription.domain.services.SubscriptionDetail;
import pe.smartcash.cash.subscription.domain.services.SubscriptionQueryService;

@Service
@Transactional(readOnly = true)
class SubscriptionQueryServiceImpl implements SubscriptionQueryService {

  private final SubscriptionRepository subscriptionRepository;

  SubscriptionQueryServiceImpl(SubscriptionRepository subscriptionRepository) {
    this.subscriptionRepository = subscriptionRepository;
  }

  @Override
  public Optional<SubscriptionDetail> handle(FindActiveSubscriptionByUserIdQuery query) {
    return subscriptionRepository.findActiveByUserId(query.userId()).map(this::toDetail);
  }

  private SubscriptionDetail toDetail(Subscription subscription) {
    return new SubscriptionDetail(
        subscription.id(),
        subscription.userId(),
        subscription.planCode(),
        subscription.status(),
        subscription.startedAt(),
        subscription.renewsAt(),
        subscription.canceledAt());
  }
}
