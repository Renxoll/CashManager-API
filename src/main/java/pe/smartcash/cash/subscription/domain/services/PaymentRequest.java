package pe.smartcash.cash.subscription.domain.services;

import pe.smartcash.cash.subscription.domain.model.commands.PayPremiumCommand.ThreeDSecureParameters;
import pe.smartcash.cash.subscription.domain.model.valueobjects.PlanCode;
import pe.smartcash.cash.subscription.domain.model.valueobjects.UserId;

/** Lo que el puerto de pagos necesita para suscribir a un usuario a un plan pago. */
public record PaymentRequest(
    UserId userId, PlanCode planCode, String cardTokenId, Payer payer, ThreeDSecureParameters authentication3ds) {

  public record Payer(
      String firstName, String lastName, String email, String phoneNumber, String address, String addressCity, String countryCode) {}
}
