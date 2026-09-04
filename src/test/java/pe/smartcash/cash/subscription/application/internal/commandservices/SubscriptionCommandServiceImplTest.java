package pe.smartcash.cash.subscription.application.internal.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.smartcash.cash.subscription.domain.exception.ActiveSubscriptionAlreadyExistsException;
import pe.smartcash.cash.subscription.domain.exception.PaymentGatewayException;
import pe.smartcash.cash.subscription.domain.exception.SubscriptionNotFoundException;
import pe.smartcash.cash.subscription.domain.model.aggregates.Subscription;
import pe.smartcash.cash.subscription.domain.model.aggregates.SubscriptionRepository;
import pe.smartcash.cash.subscription.domain.model.commands.ActivateSubscriptionCommand;
import pe.smartcash.cash.subscription.domain.model.commands.CancelSubscriptionCommand;
import pe.smartcash.cash.subscription.domain.model.commands.ExpireSubscriptionCommand;
import pe.smartcash.cash.subscription.domain.model.commands.RenewSubscriptionCommand;
import pe.smartcash.cash.subscription.domain.model.commands.StartCheckoutCommand;
import pe.smartcash.cash.subscription.domain.model.commands.SubscribeCommand;
import pe.smartcash.cash.subscription.domain.model.valueobjects.PlanCode;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionId;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionStatus;
import pe.smartcash.cash.subscription.domain.model.valueobjects.UserId;
import pe.smartcash.cash.subscription.domain.services.CheckoutSession;
import pe.smartcash.cash.subscription.domain.services.SubscriptionPaymentGateway;

/**
 * Repositorio fake en memoria (mismo criterio que {@code GmailConnectionCommandServiceImplTest}):
 * lo que estos tests verifican es la interacción con el gateway de pagos alrededor de
 * cancelar/activar, más clara con estado real que con una cadena de stubs de Mockito.
 */
class SubscriptionCommandServiceImplTest {

  private final List<Subscription> store = new ArrayList<>();
  private final SubscriptionRepository repository = new FakeRepository();
  private final SubscriptionPaymentGateway paymentGateway = mock(SubscriptionPaymentGateway.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
  private SubscriptionCommandServiceImpl service;

  private final UserId userId = UserId.of(UUID.randomUUID());

  @BeforeEach
  void setUp() {
    service = new SubscriptionCommandServiceImpl(repository, paymentGateway, clock);
  }

  @Test
  void subscribingToFreeActivatesImmediatelyWithoutTouchingThePaymentGateway() {
    service.handle(new SubscribeCommand(userId.value().toString(), "FREE"));

    assertThat(store).hasSize(1);
    verifyNoInteractionsWithGateway();
  }

  @Test
  void subscribingWhenAlreadyActiveIsRejected() {
    service.handle(new SubscribeCommand(userId.value().toString(), "FREE"));

    assertThatThrownBy(() -> service.handle(new SubscribeCommand(userId.value().toString(), "FREE")))
        .isInstanceOf(ActiveSubscriptionAlreadyExistsException.class);
    assertThat(store).hasSize(1);
  }

  @Test
  void startingCheckoutDelegatesToThePaymentGatewayWithoutActivatingAnything() {
    when(paymentGateway.startCheckout(userId, PlanCode.PREMIUM))
        .thenReturn(new CheckoutSession("https://checkout.stripe.com/session-123"));

    CheckoutSession session = service.handle(new StartCheckoutCommand(userId.value().toString(), "PREMIUM"));

    assertThat(session.checkoutUrl()).isEqualTo("https://checkout.stripe.com/session-123");
    assertThat(store).isEmpty();
  }

  @Test
  void activatingFromAConfirmedCheckoutPersistsTheStripeSubscriptionId() {
    service.handle(new ActivateSubscriptionCommand(userId.value().toString(), "PREMIUM", "sub_stripe_123"));

    assertThat(store).hasSize(1);
    assertThat(store.get(0).stripeSubscriptionId()).isEqualTo("sub_stripe_123");
  }

  @Test
  void activatingTwiceForTheSameUserIsANoOpTheSecondTime() {
    service.handle(new ActivateSubscriptionCommand(userId.value().toString(), "PREMIUM", "sub_stripe_123"));

    // Stripe entrega checkout.session.completed at-least-once; un segundo evento para el
    // mismo usuario ya activo no debe crear una segunda fila ni pisar la primera.
    service.handle(new ActivateSubscriptionCommand(userId.value().toString(), "PREMIUM", "sub_stripe_456"));

    assertThat(store).hasSize(1);
    assertThat(store.get(0).stripeSubscriptionId()).isEqualTo("sub_stripe_123");
  }

  @Test
  void cancelingAPremiumSubscriptionCancelsItInStripeBeforeMarkingItCanceledLocally() {
    service.handle(new ActivateSubscriptionCommand(userId.value().toString(), "PREMIUM", "sub_stripe_123"));

    service.handle(new CancelSubscriptionCommand(userId.value().toString()));

    verify(paymentGateway, times(1)).cancel("sub_stripe_123");
    assertThat(store.get(0).status()).isEqualTo(SubscriptionStatus.CANCELED);
  }

  @Test
  void cancelingAFreeSubscriptionNeverCallsThePaymentGateway() {
    service.handle(new SubscribeCommand(userId.value().toString(), "FREE"));

    service.handle(new CancelSubscriptionCommand(userId.value().toString()));

    verifyNoInteractionsWithGateway();
    assertThat(store.get(0).status()).isEqualTo(SubscriptionStatus.CANCELED);
  }

  @Test
  void ifStripeRejectsTheCancellationTheLocalSubscriptionStaysActive() {
    service.handle(new ActivateSubscriptionCommand(userId.value().toString(), "PREMIUM", "sub_stripe_123"));
    doThrow(new PaymentGatewayException("Stripe no responde", new RuntimeException()))
        .when(paymentGateway)
        .cancel(anyString());

    assertThatThrownBy(() -> service.handle(new CancelSubscriptionCommand(userId.value().toString())))
        .isInstanceOf(PaymentGatewayException.class);

    // La cancelación en nuestra BD nunca se persistió: si se hubiera guardado antes de saber
    // si Stripe aceptó, el usuario quedaría CANCELED acá mientras Stripe le sigue cobrando.
    assertThat(store.get(0).status()).isEqualTo(SubscriptionStatus.ACTIVE);
  }

  @Test
  void cancelingWithoutAnActiveSubscriptionThrowsNotFound() {
    assertThatThrownBy(() -> service.handle(new CancelSubscriptionCommand(userId.value().toString())))
        .isInstanceOf(SubscriptionNotFoundException.class);
  }

  @Test
  void renewingOnAPaidInvoiceMovesTheRenewalDateToOneTermFromNow() {
    service.handle(new ActivateSubscriptionCommand(userId.value().toString(), "PREMIUM", "sub_stripe_123"));

    service.handle(new RenewSubscriptionCommand("sub_stripe_123"));

    // El clock del test es fijo, así que esto no distingue "se movió" de "quedó igual" --
    // pero sí confirma la postcondición real de renew(): la fecha queda a un term() exacto
    // del instante de la renovación, no del instante de la activación original.
    assertThat(store.get(0).renewsAt()).isEqualTo(clock.instant().plus(PlanCode.PREMIUM.term()));
    assertThat(store.get(0).status()).isEqualTo(SubscriptionStatus.ACTIVE);
  }

  @Test
  void renewingAnUnknownStripeSubscriptionIsIgnored() {
    service.handle(new RenewSubscriptionCommand("sub_stripe_desconocido"));

    assertThat(store).isEmpty();
  }

  @Test
  void expiringOnADeletedStripeSubscriptionMarksItExpired() {
    service.handle(new ActivateSubscriptionCommand(userId.value().toString(), "PREMIUM", "sub_stripe_123"));

    service.handle(new ExpireSubscriptionCommand("sub_stripe_123"));

    assertThat(store.get(0).status()).isEqualTo(SubscriptionStatus.EXPIRED);
  }

  @Test
  void expiringASubscriptionAlreadyCanceledByTheUserIsANoOp() {
    service.handle(new ActivateSubscriptionCommand(userId.value().toString(), "PREMIUM", "sub_stripe_123"));
    service.handle(new CancelSubscriptionCommand(userId.value().toString()));

    // customer.subscription.deleted llega después, como confirmación de una cancelación que
    // el usuario ya había hecho desde la app -- no debe pisar CANCELED con EXPIRED.
    service.handle(new ExpireSubscriptionCommand("sub_stripe_123"));

    assertThat(store.get(0).status()).isEqualTo(SubscriptionStatus.CANCELED);
  }

  @Test
  void expiringAnUnknownStripeSubscriptionIsIgnored() {
    service.handle(new ExpireSubscriptionCommand("sub_stripe_desconocido"));

    assertThat(store).isEmpty();
  }

  private void verifyNoInteractionsWithGateway() {
    verify(paymentGateway, never()).cancel(anyString());
    verify(paymentGateway, never()).startCheckout(any(), any());
  }

  private class FakeRepository implements SubscriptionRepository {

    @Override
    public Optional<Subscription> findById(SubscriptionId id) {
      return store.stream().filter(s -> s.id().equals(id)).findFirst();
    }

    @Override
    public Optional<Subscription> findActiveByUserId(UserId userId) {
      return store.stream()
          .filter(s -> s.userId().equals(userId))
          .filter(s -> s.status() == SubscriptionStatus.ACTIVE)
          .findFirst();
    }

    @Override
    public Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId) {
      return store.stream().filter(s -> stripeSubscriptionId.equals(s.stripeSubscriptionId())).findFirst();
    }

    @Override
    public void save(Subscription subscription) {
      store.removeIf(s -> s.id().equals(subscription.id()));
      store.add(subscription);
    }
  }
}
