package pe.smartcash.cash.subscription.infrastructure.payment;

import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Component;
import pe.smartcash.cash.subscription.domain.exception.PaymentGatewayException;
import pe.smartcash.cash.subscription.domain.model.valueobjects.PlanCode;
import pe.smartcash.cash.subscription.domain.model.valueobjects.UserId;
import pe.smartcash.cash.subscription.domain.services.CheckoutSession;
import pe.smartcash.cash.subscription.domain.services.SubscriptionPaymentGateway;

/**
 * Único punto del contexto que importa {@code com.stripe.*}: el resto de subscription (domain
 * y application) solo conoce el puerto {@link SubscriptionPaymentGateway} y el DTO propio
 * {@link CheckoutSession}. El {@code userId} y el {@code planCode} viajan como metadata de la
 * sesión de Stripe porque son el único hilo que conecta "este pago se completó" (lo que
 * cuenta el webhook) con "activar la suscripción de qué usuario" (lo que necesita el
 * dominio) — Stripe no sabe nada de nuestro modelo de usuarios.
 *
 * <p>La sincronización con el ciclo de vida de Stripe (renovaciones, cancelaciones iniciadas
 * del lado de Stripe) vive en {@code StripeWebhookController}, que además de {@code
 * checkout.session.completed} escucha {@code invoice.paid} y {@code
 * customer.subscription.deleted}. {@code invoice.payment_failed} solo alerta (Sentry): Stripe
 * ya reintenta el cobro solo y termina resolviendo el evento con uno de esos otros dos.
 */
@Component
class StripePaymentGatewayAdapter implements SubscriptionPaymentGateway {

  private final StripeProperties properties;

  StripePaymentGatewayAdapter(StripeProperties properties) {
    this.properties = properties;
  }

  @Override
  public CheckoutSession startCheckout(UserId userId, PlanCode planCode) {
    if (planCode != PlanCode.PREMIUM) {
      throw new IllegalArgumentException("Solo PREMIUM requiere checkout de pago; " + planCode + " se activa directo, sin Stripe");
    }

    SessionCreateParams params =
        SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
            .setSuccessUrl(properties.successUrl())
            .setCancelUrl(properties.cancelUrl())
            .putMetadata("userId", userId.value().toString())
            .putMetadata("planCode", planCode.name())
            .addLineItem(SessionCreateParams.LineItem.builder().setPrice(properties.premiumPriceId()).setQuantity(1L).build())
            .build();

    try {
      Session session = Session.create(params);
      return new CheckoutSession(session.getUrl());
    } catch (StripeException e) {
      throw new PaymentGatewayException("No se pudo crear la sesión de Stripe Checkout", e);
    }
  }

  @Override
  public void cancel(String stripeSubscriptionId) {
    try {
      Subscription subscription = Subscription.retrieve(stripeSubscriptionId);
      // Idempotente a propósito: si ya está cancelada (p. ej. un reintento del caller, o
      // Stripe ya la había dado de baja por otro motivo) no hay nada que hacer -- llamar de
      // nuevo a cancel() sobre una ya cancelada es un error del lado de Stripe, no un no-op.
      if (!"canceled".equals(subscription.getStatus())) {
        subscription.cancel();
      }
    } catch (StripeException e) {
      throw new PaymentGatewayException("No se pudo cancelar la suscripción de Stripe " + stripeSubscriptionId, e);
    }
  }
}
