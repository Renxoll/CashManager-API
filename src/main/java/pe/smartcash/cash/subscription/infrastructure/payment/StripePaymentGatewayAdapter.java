package pe.smartcash.cash.subscription.infrastructure.payment;

import com.stripe.exception.StripeException;
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
 * <p>Alcance de este MVP: solo maneja la activación inicial vía {@code
 * checkout.session.completed}. Renovaciones, pagos fallidos o cancelaciones iniciadas desde
 * el lado de Stripe (en modo subscription, Stripe cobra automáticamente cada ciclo) no están
 * sincronizadas todavía — necesitarían escuchar además {@code invoice.paid},
 * {@code customer.subscription.deleted}, etc.
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
}
