package pe.smartcash.cash.gmailsync.application.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Solo prueba {@code effectiveSince} (método de paquete) -- la lógica que evita que el sync
 * relea el historial completo de una bandeja, ver {@link GmailSyncServiceImpl}.
 */
class GmailSyncServiceSinceTest {

  private static final Instant NOW = Instant.parse("2026-08-21T15:00:00Z");
  private static final Instant START_OF_CURRENT_MONTH = Instant.parse("2026-08-01T00:00:00Z");

  @Test
  void fallsBackToStartOfMonthWhenNeverSynced() {
    Instant since = GmailSyncServiceImpl.effectiveSince(null, NOW);

    assertThat(since).isEqualTo(START_OF_CURRENT_MONTH);
  }

  @Test
  void fallsBackToStartOfMonthWhenLastSyncedBeforeThisMonth() {
    Instant lastSyncedAt = Instant.parse("2026-07-15T10:00:00Z");

    Instant since = GmailSyncServiceImpl.effectiveSince(lastSyncedAt, NOW);

    assertThat(since).isEqualTo(START_OF_CURRENT_MONTH);
  }

  @Test
  void keepsLastSyncedAtWhenAlreadyWithinCurrentMonth() {
    Instant lastSyncedAt = Instant.parse("2026-08-20T09:00:00Z");

    Instant since = GmailSyncServiceImpl.effectiveSince(lastSyncedAt, NOW);

    assertThat(since).isEqualTo(lastSyncedAt);
  }
}
