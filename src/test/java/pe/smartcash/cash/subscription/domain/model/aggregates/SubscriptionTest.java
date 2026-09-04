package pe.smartcash.cash.subscription.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.smartcash.cash.subscription.domain.model.valueobjects.PlanCode;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionId;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionStatus;
import pe.smartcash.cash.subscription.domain.model.valueobjects.UserId;

class SubscriptionTest {

  private final UserId userId = UserId.of(UUID.randomUUID());
  private final Instant now = Instant.parse("2026-01-01T00:00:00Z");

  @Test
  void subscribingToFreeHasNoStripeSubscriptionIdAndNeverExpires() {
    Subscription subscription = Subscription.subscribe(SubscriptionId.newId(), userId, PlanCode.FREE, now);

    assertThat(subscription.status()).isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(subscription.stripeSubscriptionId()).isNull();
    assertThat(subscription.renewsAt()).isNull();
  }

  @Test
  void subscribingToPremiumKeepsTheStripeSubscriptionIdAndSetsRenewalDate() {
    Subscription subscription = Subscription.subscribe(SubscriptionId.newId(), userId, PlanCode.PREMIUM, now, "sub_stripe_123");

    assertThat(subscription.status()).isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(subscription.stripeSubscriptionId()).isEqualTo("sub_stripe_123");
    assertThat(subscription.renewsAt()).isEqualTo(now.plus(PlanCode.PREMIUM.term()));
  }

  @Test
  void cancelingAnActiveSubscriptionMarksItCanceledAndKeepsTheStripeSubscriptionId() {
    Subscription subscription = Subscription.subscribe(SubscriptionId.newId(), userId, PlanCode.PREMIUM, now, "sub_stripe_123");

    subscription.cancel(now.plusSeconds(60));

    assertThat(subscription.status()).isEqualTo(SubscriptionStatus.CANCELED);
    assertThat(subscription.canceledAt()).isEqualTo(now.plusSeconds(60));
    assertThat(subscription.stripeSubscriptionId()).isEqualTo("sub_stripe_123");
  }

  @Test
  void cancelingATwiceRejectsTheSecondAttempt() {
    Subscription subscription = Subscription.subscribe(SubscriptionId.newId(), userId, PlanCode.FREE, now);
    subscription.cancel(now.plusSeconds(60));

    assertThatThrownBy(() -> subscription.cancel(now.plusSeconds(120))).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void expiringAnActiveSubscriptionMarksItExpiredNotCanceled() {
    Subscription subscription = Subscription.subscribe(SubscriptionId.newId(), userId, PlanCode.PREMIUM, now, "sub_stripe_123");

    subscription.expire(now.plusSeconds(60));

    assertThat(subscription.status()).isEqualTo(SubscriptionStatus.EXPIRED);
    assertThat(subscription.canceledAt()).isEqualTo(now.plusSeconds(60));
  }

  @Test
  void expiringATerminalSubscriptionIsRejected() {
    Subscription subscription = Subscription.subscribe(SubscriptionId.newId(), userId, PlanCode.PREMIUM, now, "sub_stripe_123");
    subscription.cancel(now.plusSeconds(60));

    assertThatThrownBy(() -> subscription.expire(now.plusSeconds(120))).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void renewingAnActiveSubscriptionMovesTheRenewalDateForwardFromNow() {
    Subscription subscription = Subscription.subscribe(SubscriptionId.newId(), userId, PlanCode.PREMIUM, now, "sub_stripe_123");

    subscription.renew(now.plusSeconds(60));

    assertThat(subscription.renewsAt()).isEqualTo(now.plusSeconds(60).plus(PlanCode.PREMIUM.term()));
  }

  @Test
  void renewingATerminalSubscriptionIsRejected() {
    Subscription subscription = Subscription.subscribe(SubscriptionId.newId(), userId, PlanCode.PREMIUM, now, "sub_stripe_123");
    subscription.cancel(now.plusSeconds(60));

    assertThatThrownBy(() -> subscription.renew(now.plusSeconds(120))).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void renewingAPlanWithoutATermIsRejected() {
    Subscription subscription = Subscription.subscribe(SubscriptionId.newId(), userId, PlanCode.FREE, now);

    assertThatThrownBy(() -> subscription.renew(now.plusSeconds(60))).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rehydrateRestoresTheStripeSubscriptionId() {
    Subscription subscription =
        Subscription.rehydrate(
            SubscriptionId.newId(), userId, PlanCode.PREMIUM, SubscriptionStatus.ACTIVE, now, now.plusSeconds(60), null, "sub_stripe_123");

    assertThat(subscription.stripeSubscriptionId()).isEqualTo("sub_stripe_123");
  }
}
