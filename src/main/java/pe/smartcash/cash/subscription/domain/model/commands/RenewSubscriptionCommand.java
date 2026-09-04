package pe.smartcash.cash.subscription.domain.model.commands;

/**
 * Lo despacha únicamente {@code StripeWebhookController} al recibir {@code invoice.paid} --
 * Stripe cobró el ciclo siguiente de una suscripción en modo "subscription". Se identifica
 * por {@code stripeSubscriptionId} (no por userId): este evento no trae metadata propia, solo
 * la Subscription de Stripe que generó la factura.
 */
public record RenewSubscriptionCommand(String stripeSubscriptionId) {}
