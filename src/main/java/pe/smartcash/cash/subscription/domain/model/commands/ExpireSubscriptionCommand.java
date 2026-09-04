package pe.smartcash.cash.subscription.domain.model.commands;

/**
 * Lo despacha únicamente {@code StripeWebhookController} al recibir {@code
 * customer.subscription.deleted} -- Stripe dio de baja la suscripción recurrente (reintentos
 * de cobro agotados, o se canceló directo desde el dashboard de Stripe). Idempotente: si ya
 * está CANCELED (el usuario la había cancelado desde la app, lo que también la cancela en
 * Stripe y dispara este mismo evento) o ya EXPIRED, es un no-op.
 */
public record ExpireSubscriptionCommand(String stripeSubscriptionId) {}
