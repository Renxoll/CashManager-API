package pe.smartcash.cash.gmailsync.application.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.smartcash.cash.gmailsync.domain.exception.GmailAuthorizationException;
import pe.smartcash.cash.gmailsync.domain.model.aggregates.GmailConnection;
import pe.smartcash.cash.gmailsync.domain.model.aggregates.GmailConnectionRepository;
import pe.smartcash.cash.gmailsync.domain.model.valueobjects.GmailConnectionId;
import pe.smartcash.cash.gmailsync.domain.model.valueobjects.UserId;
import pe.smartcash.cash.gmailsync.domain.services.BankNotificationIngestionPort;
import pe.smartcash.cash.gmailsync.domain.services.GmailMessage;
import pe.smartcash.cash.gmailsync.domain.services.GmailMessagePort;
import pe.smartcash.cash.gmailsync.domain.services.GmailSyncResult;
import pe.smartcash.cash.gmailsync.domain.services.GoogleOAuthPort;
import pe.smartcash.cash.gmailsync.domain.services.OAuthTokens;

/** Fakes en memoria (mismo criterio que el resto de unit tests del proyecto: objetos reales, no Mockito). */
class GmailSyncServiceAuthFailureTest {

  private final Instant now = Instant.parse("2026-09-07T12:00:00Z");
  private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
  private final FakeConnectionRepository connections = new FakeConnectionRepository();
  private final ThrowingGmailPort gmailPort = new ThrowingGmailPort();
  private final GmailSyncServiceImpl service =
      new GmailSyncServiceImpl(connections, new UnusedOAuthPort(), gmailPort, new NoopIngestionPort(), clock, Set.of("bcp.com.pe"));

  private GmailConnection healthyConnection() {
    return GmailConnection.connect(
        GmailConnectionId.newId(), UserId.of(UUID.randomUUID()), "u@gmail.com", "access", "refresh", now.plusSeconds(3600), now);
  }

  @Test
  void anAuthErrorFromGmailMarksTheConnectionForReconnect() {
    connections.store.add(healthyConnection());
    gmailPort.mode = ThrowingGmailPort.Mode.AUTH_ERROR;

    GmailSyncResult result = service.syncAll();

    assertThat(result.connectionsSynced()).isZero();
    assertThat(connections.store.get(0).needsReconnect()).isTrue();
  }

  @Test
  void aConnectionAlreadyMarkedIsSkippedWithoutHittingGmail() {
    GmailConnection flagged = healthyConnection();
    flagged.recordAuthFailure(now.minusSeconds(600));
    connections.store.add(flagged);
    gmailPort.mode = ThrowingGmailPort.Mode.AUTH_ERROR; // no debería llegar a llamarse

    GmailSyncResult result = service.syncAll();

    assertThat(result.connectionsSynced()).isZero();
    assertThat(gmailPort.calls).isZero();
  }

  @Test
  void aSuccessfulPollClearsAPreviousMark() {
    GmailConnection recovered = healthyConnection();
    recovered.recordAuthFailure(now.minusSeconds(600));
    // Simula que el usuario reconectó: se limpia el flag para que el poll vuelva a tomarla.
    recovered.recordSync(now.minusSeconds(300));
    connections.store.add(recovered);
    gmailPort.mode = ThrowingGmailPort.Mode.EMPTY;

    GmailSyncResult result = service.syncAll();

    assertThat(result.connectionsSynced()).isEqualTo(1);
    assertThat(connections.store.get(0).needsReconnect()).isFalse();
  }

  // --- fakes ---

  private static final class FakeConnectionRepository implements GmailConnectionRepository {
    private final List<GmailConnection> store = new ArrayList<>();

    @Override
    public void save(GmailConnection connection) {
      store.removeIf(c -> c.id().equals(connection.id()));
      store.add(connection);
    }

    @Override
    public List<GmailConnection> findAll() {
      return List.copyOf(store);
    }

    @Override
    public List<GmailConnection> findAllByUserId(UserId userId) {
      return store.stream().filter(c -> c.userId().equals(userId)).toList();
    }

    @Override
    public Optional<GmailConnection> findById(GmailConnectionId id) {
      return store.stream().filter(c -> c.id().equals(id)).findFirst();
    }

    @Override
    public Optional<GmailConnection> findByUserIdAndEmail(UserId userId, String email) {
      return Optional.empty();
    }

    @Override
    public void delete(GmailConnectionId id) {
      store.removeIf(c -> c.id().equals(id));
    }
  }

  private static final class ThrowingGmailPort implements GmailMessagePort {
    enum Mode {
      EMPTY,
      AUTH_ERROR
    }

    private Mode mode = Mode.EMPTY;
    private int calls = 0;

    @Override
    public List<GmailMessage> findMatchingMessagesSince(String accessToken, Instant since, Set<String> senderDomains) {
      calls++;
      if (mode == Mode.AUTH_ERROR) {
        throw new GmailAuthorizationException("403 insufficient scopes");
      }
      return List.of();
    }

    @Override
    public List<GmailMessage> findCandidateMessagesSince(String accessToken, Instant since, Set<String> excludeDomains) {
      calls++;
      return List.of();
    }
  }

  private static final class NoopIngestionPort implements BankNotificationIngestionPort {
    @Override
    public void ingest(String userId, String rawText) {}

    @Override
    public boolean isTrustedSender(String userId, String fromAddress) {
      return true;
    }

    @Override
    public void recordPendingSender(String userId, String fromAddress, String rawText) {}
  }

  private static final class UnusedOAuthPort implements GoogleOAuthPort {
    @Override
    public OAuthTokens exchangeCode(String authorizationCode) {
      throw new UnsupportedOperationException();
    }

    @Override
    public OAuthTokens refresh(String refreshToken) {
      throw new UnsupportedOperationException("no debería refrescar: el token no está por vencer en el test");
    }

    @Override
    public String buildAuthorizationUrl(String state) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String fetchEmail(String accessToken) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void revoke(String token) {
      throw new UnsupportedOperationException();
    }
  }
}
