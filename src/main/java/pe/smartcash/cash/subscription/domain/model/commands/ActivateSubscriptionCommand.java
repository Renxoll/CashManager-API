package pe.smartcash.cash.subscription.domain.model.commands;

/**
 * Lo despacha únicamente {@code StripeWebhookController} al recibir {@code
 * checkout.session.completed} confirmado — nunca un cliente directo: activar sin pago
 * verificado sería regalar PREMIUM. {@code stripeSubscriptionId} es el id de la suscripción
 * que Stripe crea junto con el pago (Checkout en modo "subscription") -- se persiste desde
 * acá porque es lo único que permite cancelarla más adelante vía la API de Stripe.
 */
public record ActivateSubscriptionCommand(String userId, String planCode, String stripeSubscriptionId) {}
