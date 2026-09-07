package pe.smartcash.cash.gmailsync.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.smartcash.cash.gmailsync.domain.model.valueobjects.GmailConnectionId;
import pe.smartcash.cash.gmailsync.domain.model.valueobjects.GmailSyncError;
import pe.smartcash.cash.gmailsync.domain.model.valueobjects.UserId;

class GmailConnectionTest {

  private final Instant now = Instant.parse("2026-09-07T00:00:00Z");

  private GmailConnection connected() {
    return GmailConnection.connect(
        GmailConnectionId.newId(),
        UserId.of(UUID.randomUUID()),
        "user@gmail.com",
        "access",
        "refresh",
        now.plusSeconds(3600),
        now);
  }

  @Test
  void aFreshConnectionIsHealthy() {
    GmailConnection connection = connected();

    assertThat(connection.needsReconnect()).isFalse();
    assertThat(connection.syncError()).isNull();
  }

  @Test
  void recordAuthFailureMarksItForReconnect() {
    GmailConnection connection = connected();

    connection.recordAuthFailure(now.plusSeconds(60));

    assertThat(connection.needsReconnect()).isTrue();
    assertThat(connection.syncError()).isEqualTo(GmailSyncError.NEEDS_RECONNECT);
    assertThat(connection.syncErrorAt()).isEqualTo(now.plusSeconds(60));
  }

  @Test
  void aSuccessfulSyncClearsAPreviousAuthFailure() {
    GmailConnection connection = connected();
    connection.recordAuthFailure(now.plusSeconds(60));

    connection.recordSync(now.plusSeconds(120));

    assertThat(connection.needsReconnect()).isFalse();
    assertThat(connection.syncError()).isNull();
    assertThat(connection.syncErrorAt()).isNull();
    assertThat(connection.lastSyncedAt()).isEqualTo(now.plusSeconds(120));
  }
}
