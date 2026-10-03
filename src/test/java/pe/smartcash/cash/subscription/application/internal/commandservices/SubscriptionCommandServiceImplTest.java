package pe.smartcash.cash.subscription.application.internal.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionOperations;
import pe.smartcash.cash.subscription.domain.exception.ActiveSubscriptionAlreadyExistsException;
import pe.smartcash.cash.subscription.domain.exception.PaymentDeclinedException;
import pe.smartcash.cash.subscription.domain.exception.PaymentGatewayException;
import pe.smartcash.cash.subscription.domain.exception.SubscriptionNotFoundException;
import pe.smartcash.cash.subscription.domain.model.aggregates.Subscription;
import pe.smartcash.cash.subscription.domain.model.aggregates.SubscriptionRepository;
import pe.smartcash.cash.subscription.domain.model.commands.CancelSubscriptionCommand;
import pe.smartcash.cash.subscription.domain.model.commands.HandlePaymentEventCommand;
import pe.smartcash.cash.subscription.domain.model.commands.PayPremiumCommand;
import pe.smartcash.cash.subscription.domain.model.commands.SubscribeCommand;
import pe.smartcash.cash.subscription.domain.model.valueobjects.PlanCode;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionId;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionStatus;
import pe.smartcash.cash.subscription.domain.model.valueobjects.UserId;
import pe.smartcash.cash.subscription.domain.services.PaymentRequest;
import pe.smartcash.cash.subscription.domain.services.PaymentResult;
import pe.smartcash.cash.subscription.domain.services.PremiumPaymentOutcome;
import pe.smartcash.cash.subscription.domain.services.SubscriptionPaymentGateway;
import pe.smartcash.cash.subscription.domain.services.VerifiedPaymentEvent;

/**
 * Repositorio fake en memoria (mismo criterio que {@code GmailConnectionCommandServiceImplTest}):
 * lo que estos tests verifican es la interacción con el proveedor de pagos alrededor de pagar,
 * cancelar y sincronizar por webhook, más clara con estado real que con cadenas de stubs.
 * Las transacciones se ejecutan sin transacción real ({@link TransactionOperations#withoutTransaction()}).
 */
class SubscriptionCommandServiceImplTest {

  private final List<Subscription> store = new ArrayList<>();
  private final FakeRepository repository = new FakeRepository();
  private final SubscriptionPaymentGateway paymentGateway = mock(SubscriptionPaymentGateway.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
  private SubscriptionCommandServiceImpl service;

  private final UserId userId = UserId.of(UUID.randomUUID());

  @BeforeEach
  void setUp() {
    service = new SubscriptionCommandServiceImpl(repository, paymentGateway, TransactionOperations.withoutTransaction(), clock);
  }

  // ---- FREE ----

  @Test
  void subscribingToFreeActivatesImmediatelyWithoutTouchingThePaymentGateway() {
    service.handle(new SubscribeCommand(userId.value().toString(), "FREE"));

    assertThat(store).singleElement().extracting(Subscription::planCode).isEqualTo(PlanCode.FREE);
    verify(paymentGateway, never()).subscribe(any());
  }

  @Test
  void subscribingWhenAlreadyActiveIsRejected() {
    service.handle(new SubscribeCommand(userId.value().toString(), "FREE"));

    assertThatThrownBy(() -> service.handle(new SubscribeCommand(userId.value().toString(), "FREE")))
        .isInstanceOf(ActiveSubscriptionAlreadyExistsException.class);
    assertThat(store).hasSize(1);
  }

  // ---- Pagar PREMIUM ----

  @Test
  void aSubscribedPaymentActivatesPremiumWithTheProviderSubscriptionId() {
    when(paymentGateway.subscribe(any())).thenReturn(new PaymentResult.Subscribed("sxn_test_1"));

    PremiumPaymentOutcome outcome = service.handle(payPremium(null));

    assertThat(outcome).isEqualTo(PremiumPaymentOutcome.ACTIVATED);
    assertThat(repository.findActiveByUserId(userId)).hasValueSatisfying(active -> {
      assertThat(active.planCode()).isEqualTo(PlanCode.PREMIUM);
      assertThat(active.providerSubscriptionId()).isEqualTo("sxn_test_1");
      assertThat(active.renewsAt()).isEqualTo(clock.instant().plus(PlanCode.PREMIUM.term()));
    });
  }

  @Test
  void thePaymentRequestCarriesTheTokenThePayerAndTheUser() {
    when(paymentGateway.subscribe(any())).thenReturn(new PaymentResult.Subscribed("sxn_test_1"));

    service.handle(payPremium(null));

    ArgumentCaptor<PaymentRequest> request = ArgumentCaptor.forClass(PaymentRequest.class);
    verify(paymentGateway).subscribe(request.capture());
    assertThat(request.getValue().userId()).isEqualTo(userId);
    assertThat(request.getValue().planCode()).isEqualTo(PlanCode.PREMIUM);
    assertThat(request.getValue().cardTokenId()).isEqualTo("tkn_test_1");
    assertThat(request.getValue().payer().email()).isEqualTo("ana@example.com");
    assertThat(request.getValue().authentication3ds()).isNull();
  }

  @Test
  void payingWhileOnFreeUpgradesToPremiumAndClosesTheFreePlan() {
    service.handle(new SubscribeCommand(userId.value().toString(), "FREE"));
    when(paymentGateway.subscribe(any())).thenReturn(new PaymentResult.Subscribed("sxn_test_1"));

    service.handle(payPremium(null));

    assertThat(store).hasSize(2);
    assertThat(repository.findActiveByUserId(userId)).hasValueSatisfying(a -> assertThat(a.planCode()).isEqualTo(PlanCode.PREMIUM));
    assertThat(store).filteredOn(s -> s.planCode() == PlanCode.FREE).singleElement()
        .extracting(Subscription::status).isEqualTo(SubscriptionStatus.CANCELED);
  }

  @Test
  void whenTheBankAsksFor3dsNothingIsActivatedAndTheFreePlanStays() {
    service.handle(new SubscribeCommand(userId.value().toString(), "FREE"));
    when(paymentGateway.subscribe(any())).thenReturn(new PaymentResult.AuthenticationRequired());

    PremiumPaymentOutcome outcome = service.handle(payPremium(null));

    assertThat(outcome).isEqualTo(PremiumPaymentOutcome.AUTHENTICATION_REQUIRED);
    assertThat(repository.findActiveByUserId(userId)).hasValueSatisfying(a -> assertThat(a.planCode()).isEqualTo(PlanCode.FREE));
  }

  @Test
  void theRetryAfter3dsSendsTheAuthenticationParametersToTheProvider() {
    when(paymentGateway.subscribe(any())).thenReturn(new PaymentResult.Subscribed("sxn_test_1"));
    var threeDs = new PayPremiumCommand.ThreeDSecureParameters("05", "xid", "cavv", "2.1.0", "ds-1");

    service.handle(payPremium(threeDs));

    ArgumentCaptor<PaymentRequest> request = ArgumentCaptor.forClass(PaymentRequest.class);
    verify(paymentGateway).subscribe(request.capture());
    assertThat(request.getValue().authentication3ds()).isEqualTo(threeDs);
  }

  @Test
  void aDeclinedCardThrowsWithTheProviderMessageAndActivatesNothing() {
    when(paymentGateway.subscribe(any())).thenReturn(new PaymentResult.Declined("Tu tarjeta no tiene fondos suficientes."));

    assertThatThrownBy(() -> service.handle(payPremium(null)))
        .isInstanceOf(PaymentDeclinedException.class)
        .hasMessage("Tu tarjeta no tiene fondos suficientes.");
    assertThat(store).isEmpty();
  }

  @Test
  void payingWhileAlreadyPremiumIsRejectedWithoutCallingTheProvider() {
    store.add(Subscription.subscribe(SubscriptionId.newId(), userId, PlanCode.PREMIUM, clock.instant(), "sxn_test_0"));

    assertThatThrownBy(() -> service.handle(payPremium(null))).isInstanceOf(ActiveSubscriptionAlreadyExistsException.class);
    verify(paymentGateway, never()).subscribe(any());
  }

  @Test
  void payingWithoutAcceptingTheTermsIsRejectedWithoutCallingTheProvider() {
    PayPremiumCommand withoutTerms = withTerms(payPremium(null), false);

    assertThatThrownBy(() -> service.handle(withoutTerms)).isInstanceOf(IllegalArgumentException.class);
    verify(paymentGateway, never()).subscribe(any());
  }

  @Test
  void payingForTheFreePlanIsRejected() {
    PayPremiumCommand free = new PayPremiumCommand(
        userId.value().toString(), "FREE", "tkn_test_1", "Ana", "Pérez", "ana@example.com", "999888777", "Av. Arequipa 123", "Lima", "PE", true, null);

    assertThatThrownBy(() -> service.handle(free)).isInstanceOf(IllegalArgumentException.class);
    verify(paymentGateway, never()).subscribe(any());
  }

  @Test
  void aProviderFailurePropagatesAndActivatesNothing() {
    when(paymentGateway.subscribe(any())).thenThrow(new PaymentGatewayException("Culqi caído", new RuntimeException()));

    assertThatThrownBy(() -> service.handle(payPremium(null))).isInstanceOf(PaymentGatewayException.class);
    assertThat(store).isEmpty();
  }

  @Test
  void ifTheLocalActivationFailsAfterTheProviderChargedTheProviderSubscriptionIsCanceled() {
    when(paymentGateway.subscribe(any())).thenReturn(new PaymentResult.Subscribed("sxn_test_1"));
    repository.failNextSave = true;

    assertThatThrownBy(() -> service.handle(payPremium(null))).isInstanceOf(IllegalStateException.class);

    // Sin esto el proveedor le seguiría cobrando cada mes a alguien que quedó sin Premium.
    verify(paymentGateway).cancel("sxn_test_1");
  }

  @Test
  void ifAnotherPaymentWonTheRaceTheDuplicateProviderSubscriptionIsCanceled() {
    // Dos pestañas pagando a la vez: cuando esta llega a activar, la otra ya dejó un PREMIUM activo.
    when(paymentGateway.subscribe(any())).thenAnswer(invocation -> {
      store.add(Subscription.subscribe(SubscriptionId.newId(), userId, PlanCode.PREMIUM, clock.instant(), "sxn_test_otra"));
      return new PaymentResult.Subscribed("sxn_test_1");
    });

    assertThatThrownBy(() -> service.handle(payPremium(null))).isInstanceOf(ActiveSubscriptionAlreadyExistsException.class);

    verify(paymentGateway).cancel("sxn_test_1");
    assertThat(store).singleElement().extracting(Subscription::providerSubscriptionId).isEqualTo("sxn_test_otra");
  }

  // ---- Cancelar ----

  @Test
  void cancelingPremiumCancelsItInTheProviderBeforeMarkingItCanceledLocally() {
    store.add(Subscription.subscribe(SubscriptionId.newId(), userId, PlanCode.PREMIUM, clock.instant(), "sxn_test_1"));

    service.handle(new CancelSubscriptionCommand(userId.value().toString()));

    verify(paymentGateway).cancel("sxn_test_1");
    assertThat(store.get(0).status()).isEqualTo(SubscriptionStatus.CANCELED);
  }

  @Test
  void cancelingFreeNeverCallsTheProvider() {
    service.handle(new SubscribeCommand(userId.value().toString(), "FREE"));

    service.handle(new CancelSubscriptionCommand(userId.value().toString()));

    verify(paymentGateway, never()).cancel(anyString());
    assertThat(store.get(0).status()).isEqualTo(SubscriptionStatus.CANCELED);
  }

  @Test
  void ifTheProviderRejectsTheCancellationTheLocalSubscriptionStaysActive() {
    store.add(Subscription.subscribe(SubscriptionId.newId(), userId, PlanCode.PREMIUM, clock.instant(), "sxn_test_1"));
    doThrow(new PaymentGatewayException("Culqi caído", new RuntimeException())).when(paymentGateway).cancel(anyString());

    assertThatThrownBy(() -> service.handle(new CancelSubscriptionCommand(userId.value().toString())))
        .isInstanceOf(PaymentGatewayException.class);

    assertThat(store.get(0).status()).isEqualTo(SubscriptionStatus.ACTIVE);
  }

  @Test
  void cancelingWithoutAnActiveSubscriptionThrowsNotFound() {
    assertThatThrownBy(() -> service.handle(new CancelSubscriptionCommand(userId.value().toString())))
        .isInstanceOf(SubscriptionNotFoundException.class);
  }

  // ---- Webhooks verificados ----

  @Test
  void aVerifiedChargeSucceededEventRenewsThePremiumSubscription() {
    Instant activatedAt = clock.instant().minus(Duration.ofDays(29));
    store.add(Subscription.subscribe(SubscriptionId.newId(), userId, PlanCode.PREMIUM, activatedAt, "sxn_test_1"));
    givenVerifiedEvent("evt_1", VerifiedPaymentEvent.Type.CHARGE_SUCCEEDED, "sxn_test_1");

    service.handle(new HandlePaymentEventCommand("evt_1"));

    assertThat(store.get(0).renewsAt()).isEqualTo(clock.instant().plus(PlanCode.PREMIUM.term()));
  }

  @Test
  void aVerifiedCancelEventExpiresThePremiumSubscription() {
    store.add(Subscription.subscribe(SubscriptionId.newId(), userId, PlanCode.PREMIUM, clock.instant(), "sxn_test_1"));
    givenVerifiedEvent("evt_1", VerifiedPaymentEvent.Type.SUBSCRIPTION_CANCELED, "sxn_test_1");

    service.handle(new HandlePaymentEventCommand("evt_1"));

    assertThat(store.get(0).status()).isEqualTo(SubscriptionStatus.EXPIRED);
  }

  @Test
  void aCancelEventForASubscriptionTheUserAlreadyCanceledKeepsItCanceled() {
    store.add(Subscription.subscribe(SubscriptionId.newId(), userId, PlanCode.PREMIUM, clock.instant(), "sxn_test_1"));
    service.handle(new CancelSubscriptionCommand(userId.value().toString()));
    givenVerifiedEvent("evt_1", VerifiedPaymentEvent.Type.SUBSCRIPTION_CANCELED, "sxn_test_1");

    service.handle(new HandlePaymentEventCommand("evt_1"));

    assertThat(store.get(0).status()).isEqualTo(SubscriptionStatus.CANCELED);
  }

  @Test
  void aChargeFailedEventOnlyAlertsAndKeepsThePlanActive() {
    store.add(Subscription.subscribe(SubscriptionId.newId(), userId, PlanCode.PREMIUM, clock.instant(), "sxn_test_1"));
    givenVerifiedEvent("evt_1", VerifiedPaymentEvent.Type.CHARGE_FAILED, "sxn_test_1");

    service.handle(new HandlePaymentEventCommand("evt_1"));

    assertThat(store.get(0).status()).isEqualTo(SubscriptionStatus.ACTIVE);
  }

  @Test
  void anEventTheProviderDoesNotConfirmChangesNothing() {
    store.add(Subscription.subscribe(SubscriptionId.newId(), userId, PlanCode.PREMIUM, clock.instant(), "sxn_test_1"));
    when(paymentGateway.verifyEvent("evt_inventado")).thenReturn(Optional.empty());

    service.handle(new HandlePaymentEventCommand("evt_inventado"));

    assertThat(store.get(0).status()).isEqualTo(SubscriptionStatus.ACTIVE);
  }

  @Test
  void anEventForAnUnknownProviderSubscriptionIsIgnored() {
    givenVerifiedEvent("evt_1", VerifiedPaymentEvent.Type.SUBSCRIPTION_CANCELED, "sxn_test_desconocida");

    service.handle(new HandlePaymentEventCommand("evt_1"));

    assertThat(store).isEmpty();
  }

  private void givenVerifiedEvent(String eventId, VerifiedPaymentEvent.Type type, String providerSubscriptionId) {
    when(paymentGateway.verifyEvent(eventId)).thenReturn(Optional.of(new VerifiedPaymentEvent(eventId, type, providerSubscriptionId)));
  }

  private PayPremiumCommand payPremium(PayPremiumCommand.ThreeDSecureParameters threeDs) {
    return new PayPremiumCommand(
        userId.value().toString(),
        "PREMIUM",
        "tkn_test_1",
        "Ana",
        "Pérez",
        "ana@example.com",
        "999888777",
        "Av. Arequipa 123",
        "Lima",
        "PE",
        true,
        threeDs);
  }

  private static PayPremiumCommand withTerms(PayPremiumCommand c, boolean accepted) {
    return new PayPremiumCommand(
        c.userId(), c.planCode(), c.cardTokenId(), c.firstName(), c.lastName(), c.email(), c.phoneNumber(), c.address(),
        c.addressCity(), c.countryCode(), accepted, c.authentication3ds());
  }

  private class FakeRepository implements SubscriptionRepository {

    boolean failNextSave;

    @Override
    public Optional<Subscription> findById(SubscriptionId id) {
      return store.stream().filter(s -> s.id().equals(id)).findFirst();
    }

    @Override
    public Optional<Subscription> findActiveByUserId(UserId userId) {
      return store.stream().filter(s -> s.userId().equals(userId)).filter(s -> s.status() == SubscriptionStatus.ACTIVE).findFirst();
    }

    @Override
    public Optional<Subscription> findByProviderSubscriptionId(String providerSubscriptionId) {
      return store.stream().filter(s -> providerSubscriptionId.equals(s.providerSubscriptionId())).findFirst();
    }

    @Override
    public void save(Subscription subscription) {
      if (failNextSave) {
        failNextSave = false;
        throw new IllegalStateException("BD caída");
      }
      store.removeIf(s -> s.id().equals(subscription.id()));
      store.add(subscription);
    }
  }
}
