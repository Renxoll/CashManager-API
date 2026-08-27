package pe.smartcash.cash.gmailsync.application.internal.commandservices;

import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import pe.smartcash.cash.gmailsync.domain.model.aggregates.GmailConnection;
import pe.smartcash.cash.gmailsync.domain.model.aggregates.GmailConnectionRepository;
import pe.smartcash.cash.gmailsync.domain.model.commands.StoreGmailConnectionCommand;
import pe.smartcash.cash.gmailsync.domain.model.valueobjects.UserId;
import pe.smartcash.cash.gmailsync.domain.services.GmailConnectionCommandService;

@Service
class GmailConnectionCommandServiceImpl implements GmailConnectionCommandService {

  private final GmailConnectionRepository repository;
  private final Clock clock;

  GmailConnectionCommandServiceImpl(GmailConnectionRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @Override
  public void handle(StoreGmailConnectionCommand command) {
    UserId userId = UserId.of(UUID.fromString(command.userId()));
    String refreshToken = resolveRefreshToken(userId, command.refreshToken());
    GmailConnection connection = GmailConnection.connect(userId, command.accessToken(), refreshToken, command.accessTokenExpiresAt(), clock.instant());
    repository.save(connection);
  }

  /**
   * Google solo reemite el refresh token en el consentimiento inicial (o si se fuerza
   * {@code prompt=consent}, que es lo que hace {@code GoogleOAuthPortAdapter}) -- esto es
   * un respaldo defensivo por si algún día llega null igual: reusar el que ya había en vez
   * de romper la reconexión.
   */
  private String resolveRefreshToken(UserId userId, String newRefreshToken) {
    if (newRefreshToken != null && !newRefreshToken.isBlank()) {
      return newRefreshToken;
    }
    return repository
        .findByUserId(userId)
        .map(GmailConnection::refreshToken)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Google no devolvió un refresh_token y no había uno previo para reusar; hace falta reconectar"));
  }
}
