package pe.smartcash.cash.iam.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.smartcash.cash.iam.domain.model.valueobjects.PasswordResetTokenId;
import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;

class PasswordResetTokenTest {

  private final UserId userId = UserId.of(UUID.randomUUID());
  private final Instant now = Instant.parse("2026-01-01T00:00:00Z");
  private final Duration ttl = Duration.ofMinutes(30);

  private PasswordResetToken issue() {
    return PasswordResetToken.issue(PasswordResetTokenId.newId(), userId, "hash", now, ttl);
  }

  @Test
  void aFreshTokenIsRedeemableWithinItsWindow() {
    PasswordResetToken token = issue();

    assertThat(token.isRedeemable(now.plusSeconds(60))).isTrue();
    assertThat(token.expiresAt()).isEqualTo(now.plus(ttl));
    assertThat(token.redeemedAt()).isNull();
  }

  @Test
  void anExpiredTokenIsNotRedeemable() {
    PasswordResetToken token = issue();

    assertThat(token.isRedeemable(now.plus(ttl).plusSeconds(1))).isFalse();
  }

  @Test
  void redeemingMarksItUsedAndBlocksASecondRedeem() {
    PasswordResetToken token = issue();

    token.redeem(now.plusSeconds(60));

    assertThat(token.redeemedAt()).isEqualTo(now.plusSeconds(60));
    assertThat(token.isRedeemable(now.plusSeconds(120))).isFalse();
    assertThatThrownBy(() -> token.redeem(now.plusSeconds(120))).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void redeemingAnExpiredTokenIsRejected() {
    PasswordResetToken token = issue();

    assertThatThrownBy(() -> token.redeem(now.plus(ttl).plusSeconds(1))).isInstanceOf(IllegalStateException.class);
  }
}
