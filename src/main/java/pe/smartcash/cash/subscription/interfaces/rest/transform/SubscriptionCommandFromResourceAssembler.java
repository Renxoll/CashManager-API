package pe.smartcash.cash.subscription.interfaces.rest.transform;

import pe.smartcash.cash.subscription.domain.model.commands.CancelSubscriptionCommand;
import pe.smartcash.cash.subscription.domain.model.commands.SubscribeCommand;
import pe.smartcash.cash.subscription.interfaces.rest.resources.SubscribeResource;

public final class SubscriptionCommandFromResourceAssembler {

  private SubscriptionCommandFromResourceAssembler() {}

  public static SubscribeCommand toSubscribeCommand(String authenticatedUserId, SubscribeResource resource) {
    return new SubscribeCommand(authenticatedUserId, resource.planCode());
  }

  public static CancelSubscriptionCommand toCancelCommand(String authenticatedUserId) {
    return new CancelSubscriptionCommand(authenticatedUserId);
  }
}
