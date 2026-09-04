package pe.smartcash.cash.subscription.infrastructure.persistence;

import pe.smartcash.cash.subscription.domain.model.aggregates.Subscription;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionId;
import pe.smartcash.cash.subscription.domain.model.valueobjects.UserId;

final class SubscriptionEntityMapper {

  private SubscriptionEntityMapper() {}

  static SubscriptionJpaEntity toJpaEntity(Subscription subscription) {
    return SubscriptionJpaEntity.builder()
        .id(subscription.id().value())
        .userId(subscription.userId().value())
        .planCode(subscription.planCode())
        .status(subscription.status())
        .startedAt(subscription.startedAt())
        .renewsAt(subscription.renewsAt())
        .canceledAt(subscription.canceledAt())
        .stripeSubscriptionId(subscription.stripeSubscriptionId())
        .build();
  }

  static Subscription toDomain(SubscriptionJpaEntity entity) {
    return Subscription.rehydrate(
        SubscriptionId.of(entity.getId()),
        UserId.of(entity.getUserId()),
        entity.getPlanCode(),
        entity.getStatus(),
        entity.getStartedAt(),
        entity.getRenewsAt(),
        entity.getCanceledAt(),
        entity.getStripeSubscriptionId());
  }
}
