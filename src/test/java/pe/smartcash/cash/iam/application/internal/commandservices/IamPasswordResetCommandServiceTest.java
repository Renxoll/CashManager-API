package pe.smartcash.cash.iam.application.internal.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import pe.smartcash.cash.iam.domain.exception.InvalidPasswordResetTokenException;
import pe.smartcash.cash.iam.domain.model.aggregates.Credentials;
import pe.smartcash.cash.iam.domain.model.aggregates.CredentialsRepository;
import pe.smartcash.cash.iam.domain.model.aggregates.PasswordResetToken;
import pe.smartcash.cash.iam.domain.model.aggregates.PasswordResetTokenRepository;
import pe.smartcash.cash.iam.domain.model.commands.RequestPasswordResetCommand;
import pe.smartcash.cash.iam.domain.model.commands.ResetPasswordCommand;
import pe.smartcash.cash.iam.domain.model.valueobjects.Email;
import pe.smartcash.cash.iam.domain.model.valueobjects.HashedPassword;
import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;
import pe.smartcash.cash.iam.domain.services.PasswordHasher;
import pe.smartcash.cash.iam.domain.services.PasswordResetEmailNotifier;
import pe.smartcash.cash.iam.domain.services.PasswordResetTokenGenerator;

/**
 * Fakes en memoria (mismo criterio que {@code IamQueryServiceImplTest}: objetos reales, no
 * Mockito, para unit tests puros). Solo se arman los colaboradores que tocan los dos
 * handlers de restablecimiento; el resto va {@code null} porque esos caminos no los usan.
 */
class IamPasswordResetCommandServiceTest {

  private static final Duration TTL = Duration.ofMinutes(30);

  private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
  private final FakeCredentialsRepository credentials = new FakeCredentialsRepository();
  private final FakeResetTokenRepository resetTokens = new FakeResetTokenRepository();
  private final FakeTokenGenerator tokenGenerator = new FakeTokenGenerator();
  private final RecordingEmailNotifier emailNotifier = new RecordingEmailNotifier();
  private final PasswordHasher passwordHasher =
      new PasswordHasher() {
        @Override
        public HashedPassword hash(String rawPassword) {
          return new HashedPassword("hashed(" + rawPassword + ")");
        }

        @Override
        public boolean matches(String rawPassword, HashedPassword hashedPassword) {
          return hashedPassword.value().equals("hashed(" + rawPassword + ")");
        }
      };

  private IamCommandServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new IamCommandServiceImpl(
            credentials,
            passwordHasher,
            null,
            null,
            resetTokens,
            tokenGenerator,
            emailNotifier,
            "https://www.tuluki.com/reset-password",
            TTL,
            event -> {},
            clock);
  }

  private UserId registerUser(String email) {
    UserId userId = UserId.of(UUID.randomUUID());
    credentials.store.add(Credentials.register(userId, new Email(email), new HashedPassword("hashed(old-pass)"), clock.instant()));
    return userId;
  }

  @Test
  void requestingForAnUnknownEmailIsASilentNoOp() {
    service.handle(new RequestPasswordResetCommand("nadie@example.com"));

    assertThat(resetTokens.store).isEmpty();
    assertThat(emailNotifier.sent).isEmpty();
  }

  @Test
  void requestingForAMalformedEmailIsASilentNoOp() {
    service.handle(new RequestPasswordResetCommand("no-es-un-email"));

    assertThat(resetTokens.store).isEmpty();
    assertThat(emailNotifier.sent).isEmpty();
  }

  @Test
  void requestingForAKnownEmailStoresTheHashAndEmailsTheRawTokenLink() {
    registerUser("dueño@example.com");

    service.handle(new RequestPasswordResetCommand("dueño@example.com"));

    assertThat(resetTokens.store).hasSize(1);
    assertThat(resetTokens.store.get(0).tokenHash()).isEqualTo("hash-of-raw-token-1");
    assertThat(emailNotifier.sent).hasSize(1);
    assertThat(emailNotifier.sent.get(0).resetUrl())
        .isEqualTo("https://www.tuluki.com/reset-password?token=raw-token-1");
  }

  @Test
  void requestingAgainInvalidatesThePreviousToken() {
    registerUser("dueño@example.com");

    service.handle(new RequestPasswordResetCommand("dueño@example.com"));
    service.handle(new RequestPasswordResetCommand("dueño@example.com"));

    // El primer token se borró: solo queda el segundo.
    assertThat(resetTokens.store).hasSize(1);
    assertThat(resetTokens.store.get(0).tokenHash()).isEqualTo("hash-of-raw-token-2");
  }

  @Test
  void confirmingWithAValidTokenChangesThePasswordAndBurnsTheToken() {
    UserId userId = registerUser("dueño@example.com");
    service.handle(new RequestPasswordResetCommand("dueño@example.com"));

    service.handle(new ResetPasswordCommand("raw-token-1", "mi-nueva-clave"));

    assertThat(credentials.findById(userId).orElseThrow().hashedPassword().value()).isEqualTo("hashed(mi-nueva-clave)");
    assertThat(resetTokens.store.get(0).isRedeemable(clock.instant())).isFalse();
  }

  @Test
  void aBurnedTokenCannotBeReused() {
    registerUser("dueño@example.com");
    service.handle(new RequestPasswordResetCommand("dueño@example.com"));
    service.handle(new ResetPasswordCommand("raw-token-1", "clave-uno"));

    assertThatThrownBy(() -> service.handle(new ResetPasswordCommand("raw-token-1", "clave-dos")))
        .isInstanceOf(InvalidPasswordResetTokenException.class);
  }

  @Test
  void confirmingWithAnUnknownTokenIsRejected() {
    assertThatThrownBy(() -> service.handle(new ResetPasswordCommand("no-existe", "clave")))
        .isInstanceOf(InvalidPasswordResetTokenException.class);
  }

  @Test
  void confirmingWithAnExpiredTokenIsRejected() {
    registerUser("dueño@example.com");
    // Token emitido 40 min "antes" del clock fijo -> ya expiró (ttl = 30 min).
    resetTokens.store.add(
        PasswordResetToken.issue(
            pe.smartcash.cash.iam.domain.model.valueobjects.PasswordResetTokenId.newId(),
            credentials.store.get(0).id(),
            tokenGenerator.hash("raw-viejo"),
            clock.instant().minus(Duration.ofMinutes(40)),
            TTL));

    assertThatThrownBy(() -> service.handle(new ResetPasswordCommand("raw-viejo", "clave")))
        .isInstanceOf(InvalidPasswordResetTokenException.class);
  }

  // --- fakes ---

  private static final class FakeCredentialsRepository implements CredentialsRepository {
    private final List<Credentials> store = new ArrayList<>();

    @Override
    public Optional<Credentials> findByEmail(Email email) {
      return store.stream().filter(c -> c.email().equals(email)).findFirst();
    }

    @Override
    public Optional<Credentials> findById(UserId id) {
      return store.stream().filter(c -> c.id().equals(id)).findFirst();
    }

    @Override
    public void save(Credentials credentials) {
      store.removeIf(c -> c.id().equals(credentials.id()));
      store.add(credentials);
    }
  }

  private static final class FakeResetTokenRepository implements PasswordResetTokenRepository {
    private final List<PasswordResetToken> store = new ArrayList<>();

    @Override
    public void save(PasswordResetToken token) {
      store.removeIf(t -> t.id().equals(token.id()));
      store.add(token);
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
      return store.stream().filter(t -> t.tokenHash().equals(tokenHash)).findFirst();
    }

    @Override
    public void deleteAllByUserId(UserId userId) {
      store.removeIf(t -> t.userId().equals(userId));
    }
  }

  /** raw-token-1, raw-token-2, ... y su hash es "hash-of-<raw>". */
  private static final class FakeTokenGenerator implements PasswordResetTokenGenerator {
    private int counter = 0;

    @Override
    public GeneratedToken generate() {
      String raw = "raw-token-" + (++counter);
      return new GeneratedToken(raw, hash(raw));
    }

    @Override
    public String hash(String rawToken) {
      return "hash-of-" + rawToken;
    }
  }

  private static final class RecordingEmailNotifier implements PasswordResetEmailNotifier {
    private record Sent(String recipient, String resetUrl) {}

    private final List<Sent> sent = new ArrayList<>();

    @Override
    public void sendResetLink(Email recipient, String resetUrl, Duration validFor) {
      sent.add(new Sent(recipient.value(), resetUrl));
    }
  }
}
