package pe.smartcash.cash.gmailsync.application.internal.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.smartcash.cash.gmailsync.domain.exception.GmailConnectionNotFoundException;
import pe.smartcash.cash.gmailsync.domain.model.aggregates.GmailConnection;
import pe.smartcash.cash.gmailsync.domain.model.aggregates.GmailConnectionRepository;
import pe.smartcash.cash.gmailsync.domain.model.commands.DisconnectGmailConnectionCommand;
import pe.smartcash.cash.gmailsync.domain.model.commands.StoreGmailConnectionCommand;
import pe.smartcash.cash.gmailsync.domain.model.valueobjects.GmailConnectionId;
import pe.smartcash.cash.gmailsync.domain.model.valueobjects.UserId;
import pe.smartcash.cash.gmailsync.domain.services.GoogleOAuthPort;

/**
 * Repositorio fake en memoria (no Mockito) porque acá lo que importa es el comportamiento de
 * "buscar por (userId, email) y decidir crear vs. actualizar", que es más claro de verificar
 * con estado real que con una cadena de stubs.
 */
class GmailConnectionCommandServiceImplTest {

  private final List<GmailConnection> store = new ArrayList<>();
  private final GmailConnectionRepository repository = new FakeRepository();
  private final GoogleOAuthPort oauthPort = mock(GoogleOAuthPort.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
  private GmailConnectionCommandServiceImpl service;

  private final UserId userId = UserId.of(UUID.randomUUID());

  @BeforeEach
  void setUp() {
    service = new GmailConnectionCommandServiceImpl(repository, oauthPort, clock);
  }

  @Test
  void firstConnectionCreatesANewRow() {
    service.handle(new StoreGmailConnectionCommand(userId.value().toString(), "user@gmail.com", "access-1", "refresh-1", Instant.now()));

    assertThat(store).hasSize(1);
    assertThat(store.get(0).email()).isEqualTo("user@gmail.com");
  }

  @Test
  void reconnectingTheSameEmailReusesTheSameIdInsteadOfDuplicating() {
    service.handle(new StoreGmailConnectionCommand(userId.value().toString(), "user@gmail.com", "access-1", "refresh-1", Instant.now()));
    GmailConnectionId firstId = store.get(0).id();

    service.handle(new StoreGmailConnectionCommand(userId.value().toString(), "user@gmail.com", "access-2", "refresh-2", Instant.now()));

    assertThat(store).hasSize(1);
    assertThat(store.get(0).id()).isEqualTo(firstId);
    assertThat(store.get(0).accessToken()).isEqualTo("access-2");
  }

  @Test
  void connectingADifferentEmailCreatesASecondRow() {
    service.handle(new StoreGmailConnectionCommand(userId.value().toString(), "user@gmail.com", "access-1", "refresh-1", Instant.now()));
    service.handle(new StoreGmailConnectionCommand(userId.value().toString(), "otro@gmail.com", "access-2", "refresh-2", Instant.now()));

    assertThat(store).hasSize(2);
    assertThat(store.stream().map(GmailConnection::email)).containsExactlyInAnyOrder("user@gmail.com", "otro@gmail.com");
  }

  @Test
  void disconnectingRevokesAndDeletesTheConnection() {
    service.handle(new StoreGmailConnectionCommand(userId.value().toString(), "user@gmail.com", "access-1", "refresh-1", Instant.now()));
    GmailConnectionId id = store.get(0).id();

    service.handle(new DisconnectGmailConnectionCommand(id, userId));

    assertThat(store).isEmpty();
    verify(oauthPort, times(1)).revoke("refresh-1");
  }

  @Test
  void disconnectingSomeoneElsesConnectionThrowsNotFound() {
    service.handle(new StoreGmailConnectionCommand(userId.value().toString(), "user@gmail.com", "access-1", "refresh-1", Instant.now()));
    GmailConnectionId id = store.get(0).id();
    UserId otherUserId = UserId.of(UUID.randomUUID());

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.handle(new DisconnectGmailConnectionCommand(id, otherUserId)))
        .isInstanceOf(GmailConnectionNotFoundException.class);
    assertThat(store).hasSize(1);
  }

  @Test
  void disconnectingStillDeletesLocallyEvenIfRevokeFails() {
    doThrow(new RuntimeException("Google no responde")).when(oauthPort).revoke(anyString());
    service.handle(new StoreGmailConnectionCommand(userId.value().toString(), "user@gmail.com", "access-1", "refresh-1", Instant.now()));
    GmailConnectionId id = store.get(0).id();

    service.handle(new DisconnectGmailConnectionCommand(id, userId));

    assertThat(store).isEmpty();
  }

  private class FakeRepository implements GmailConnectionRepository {

    @Override
    public void save(GmailConnection connection) {
      store.removeIf(c -> c.id().equals(connection.id()));
      store.add(connection);
    }

    @Override
    public Optional<GmailConnection> findById(GmailConnectionId id) {
      return store.stream().filter(c -> c.id().equals(id)).findFirst();
    }

    @Override
    public List<GmailConnection> findAllByUserId(UserId userId) {
      return store.stream().filter(c -> c.userId().equals(userId)).toList();
    }

    @Override
    public Optional<GmailConnection> findByUserIdAndEmail(UserId userId, String email) {
      return store.stream().filter(c -> c.userId().equals(userId) && email.equals(c.email())).findFirst();
    }

    @Override
    public void delete(GmailConnectionId id) {
      store.removeIf(c -> c.id().equals(id));
    }

    @Override
    public List<GmailConnection> findAll() {
      return List.copyOf(store);
    }
  }
}
