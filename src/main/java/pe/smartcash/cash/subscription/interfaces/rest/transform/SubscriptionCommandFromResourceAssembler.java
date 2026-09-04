package pe.smartcash.cash.subscription.interfaces.rest.transform;

import pe.smartcash.cash.subscription.domain.model.commands.ActivateSubscriptionCommand;
import pe.smartcash.cash.subscription.domain.model.commands.CancelSubscriptionCommand;
import pe.smartcash.cash.subscription.domain.model.commands.ExpireSubscriptionCommand;
import pe.smartcash.cash.subscription.domain.model.commands.RenewSubscriptionCommand;
import pe.smartcash.cash.subscription.domain.model.commands.StartCheckoutCommand;
import pe.smartcash.cash.subscription.domain.model.commands.SubscribeCommand;
import pe.smartcash.cash.subscription.interfaces.rest.resources.SubscribeResource;

public final class SubscriptionCommandFromResourceAssembler {

  private SubscriptionCommandFromResourceAssembler() {}

  public static SubscribeCommand toSubscribeCommand(String authenticatedUserId, SubscribeResource resource) {
    return new SubscribeCommand(authenticatedUserId, resource.planCode());
  }

  public static StartCheckoutCommand toStartCheckoutCommand(String authenticatedUserId, SubscribeResource resource) {
    return new StartCheckoutCommand(authenticatedUserId, resource.planCode());
  }

  public static ActivateSubscriptionCommand toActivateSubscriptionCommand(String userId, String planCode, String stripeSubscriptionId) {
    return new ActivateSubscriptionCommand(userId, planCode, stripeSubscriptionId);
  }

  public static CancelSubscriptionCommand toCancelCommand(String authenticatedUserId) {
    return new CancelSubscriptionCommand(authenticatedUserId);
  }

  public static RenewSubscriptionCommand toRenewCommand(String stripeSubscriptionId) {
    return new RenewSubscriptionCommand(stripeSubscriptionId);
  }

  public static ExpireSubscriptionCommand toExpireCommand(String stripeSubscriptionId) {
    return new ExpireSubscriptionCommand(stripeSubscriptionId);
  }
}
