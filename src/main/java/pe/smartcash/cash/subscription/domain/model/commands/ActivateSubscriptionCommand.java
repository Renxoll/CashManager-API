package pe.smartcash.cash.subscription.domain.model.commands;

/**
 * Lo despacha únicamente {@code StripeWebhookController} al recibir {@code
 * checkout.session.completed} confirmado — nunca un cliente directo: activar sin pago
 * verificado sería regalar PREMIUM.
 */
public record ActivateSubscriptionCommand(String userId, String planCode) {}
