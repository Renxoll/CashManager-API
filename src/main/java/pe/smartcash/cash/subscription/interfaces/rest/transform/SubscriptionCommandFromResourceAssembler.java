package pe.smartcash.cash.subscription.interfaces.rest.transform;

import pe.smartcash.cash.subscription.domain.model.commands.CancelSubscriptionCommand;
import pe.smartcash.cash.subscription.domain.model.commands.HandlePaymentEventCommand;
import pe.smartcash.cash.subscription.domain.model.commands.PayPremiumCommand;
import pe.smartcash.cash.subscription.domain.model.commands.SubscribeCommand;
import pe.smartcash.cash.subscription.interfaces.rest.resources.PayPremiumResource;
import pe.smartcash.cash.subscription.interfaces.rest.resources.SubscribeResource;

public final class SubscriptionCommandFromResourceAssembler {

  private static final String DEFAULT_COUNTRY_CODE = "PE";

  private SubscriptionCommandFromResourceAssembler() {}

  public static SubscribeCommand toSubscribeCommand(String authenticatedUserId, SubscribeResource resource) {
    return new SubscribeCommand(authenticatedUserId, resource.planCode());
  }

  public static PayPremiumCommand toPayPremiumCommand(String authenticatedUserId, PayPremiumResource resource) {
    var payer = resource.payer();
    var threeDs = resource.authentication3DS();
    return new PayPremiumCommand(
        authenticatedUserId,
        resource.planCode(),
        resource.cardToken().trim(),
        payer.firstName().trim(),
        payer.lastName().trim(),
        payer.email().trim(),
        payer.phoneNumber(),
        payer.address().trim(),
        payer.addressCity().trim(),
        payer.countryCode() != null ? payer.countryCode() : DEFAULT_COUNTRY_CODE,
        resource.acceptedTerms(),
        threeDs == null
            ? null
            : new PayPremiumCommand.ThreeDSecureParameters(
                threeDs.eci(), threeDs.xid(), threeDs.cavv(), threeDs.protocolVersion(), threeDs.directoryServerTransactionId()));
  }

  public static CancelSubscriptionCommand toCancelCommand(String authenticatedUserId) {
    return new CancelSubscriptionCommand(authenticatedUserId);
  }

  public static HandlePaymentEventCommand toHandlePaymentEventCommand(String eventId) {
    return new HandlePaymentEventCommand(eventId);
  }
}
