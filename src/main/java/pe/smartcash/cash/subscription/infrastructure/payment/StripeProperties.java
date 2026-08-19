package pe.smartcash.cash.subscription.infrastructure.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Sin validación estricta a propósito (a diferencia de {@code iam.token.secret}, que sí
 * revienta el arranque si falta): Stripe es una integración opcional del contexto
 * subscription, no algo que toda la app necesite para levantar en dev/CI. Si falta,
 * {@code StripePaymentGatewayAdapter} falla recién cuando alguien intenta pagar, no antes.
 */
@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(String secretKey, String webhookSecret, String premiumPriceId, String successUrl, String cancelUrl) {}
