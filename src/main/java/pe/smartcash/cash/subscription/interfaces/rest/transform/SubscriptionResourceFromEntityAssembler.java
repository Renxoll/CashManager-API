package pe.smartcash.cash.subscription.interfaces.rest.transform;

import pe.smartcash.cash.subscription.domain.services.SubscriptionDetail;
import pe.smartcash.cash.subscription.interfaces.rest.resources.SubscriptionResource;

public final class SubscriptionResourceFromEntityAssembler {

  private SubscriptionResourceFromEntityAssembler() {}

  public static SubscriptionResource toResourceFromEntity(SubscriptionDetail detail) {
    return new SubscriptionResource(
        detail.id().value(),
        detail.userId().value(),
        detail.planCode().name(),
        detail.status().name(),
        detail.startedAt(),
        detail.renewsAt(),
        detail.canceledAt());
  }
}
