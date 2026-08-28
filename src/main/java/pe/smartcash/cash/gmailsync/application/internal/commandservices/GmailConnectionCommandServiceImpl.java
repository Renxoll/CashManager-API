package pe.smartcash.cash.gmailsync.application.internal.commandservices;

import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import pe.smartcash.cash.gmailsync.domain.exception.GmailConnectionNotFoundException;
import pe.smartcash.cash.gmailsync.domain.model.aggregates.GmailConnection;
import pe.smartcash.cash.gmailsync.domain.model.aggregates.GmailConnectionRepository;
import pe.smartcash.cash.gmailsync.domain.model.commands.DisconnectGmailConnectionCommand;
import pe.smartcash.cash.gmailsync.domain.model.commands.StoreGmailConnectionCommand;
import pe.smartcash.cash.gmailsync.domain.model.valueobjects.GmailConnectionId;
import pe.smartcash.cash.gmailsync.domain.model.valueobjects.UserId;
import pe.smartcash.cash.gmailsync.domain.services.GmailConnectionCommandService;
import pe.smartcash.cash.gmailsync.domain.services.GoogleOAuthPort;

@Service
class GmailConnectionCommandServiceImpl implements GmailConnectionCommandService {

  private final GmailConnectionRepository repository;
  private final GoogleOAuthPort oauthPort;
  private final Clock clock;

  GmailConnectionCommandServiceImpl(GmailConnectionRepository repository, GoogleOAuthPort oauthPort, Clock clock) {
    this.repository = repository;
    this.oauthPort = oauthPort;
    this.clock = clock;
  }

  /**
   * Busca explícitamente por (userId, email) -- ya no upsertea a ciegas por userId (eso
   * pisaría otra cuenta del mismo usuario). Si el email es el mismo que una conexión
   * existente, es una reautenticación: se rota el token en la misma fila/id. Si es un email
   * distinto (o no se pudo obtener el email de Google), es una cuenta nueva.
   */
  @Override
  public void handle(StoreGmailConnectionCommand command) {
    UserId userId = UserId.parse(command.userId());
    Optional<GmailConnection> existing =
        command.email() != null ? repository.findByUserIdAndEmail(userId, command.email()) : Optional.empty();

    if (existing.isPresent()) {
      GmailConnection connection = existing.get();
      String refreshToken = resolveRefreshToken(connection, command.refreshToken());
      connection.refreshAccessToken(command.accessToken(), refreshToken, command.accessTokenExpiresAt(), clock.instant());
      repository.save(connection);
      return;
    }

    GmailConnection connection =
        GmailConnection.connect(
            GmailConnectionId.newId(), userId, command.email(), command.accessToken(), command.refreshToken(), command.accessTokenExpiresAt(), clock.instant());
    repository.save(connection);
  }

  @Override
  public void handle(DisconnectGmailConnectionCommand command) {
    GmailConnection connection =
        repository
            .findById(command.connectionId())
            .filter(c -> c.userId().equals(command.requestingUserId()))
            .orElseThrow(() -> new GmailConnectionNotFoundException(command.connectionId()));
    // Best-effort: revocar en Google no debe bloquear que el usuario desconecte localmente
    // -- un token ya vencido/revocado del lado de Google fallaría acá sin que eso sea un
    // motivo real para negarle al usuario "olvidar" esta conexión.
    try {
      oauthPort.revoke(connection.refreshToken());
    } catch (Exception e) {
      // Sin logger inyectado en este service hoy; el fallo de revocación no es crítico (ver
      // comentario arriba), así que no vale la pena traer una dependencia nueva solo para
      // este catch -- si esto necesita observabilidad real, es candidato a revisar.
    }
    repository.delete(connection.id());
  }

  /**
   * Google solo reemite el refresh token en el consentimiento inicial (o si se fuerza
   * {@code prompt=consent}, que es lo que hace {@code GoogleOAuthPortAdapter}) -- esto es
   * un respaldo defensivo por si algún día llega null igual: reusar el que ya había en vez
   * de romper la reconexión.
   */
  private String resolveRefreshToken(GmailConnection existing, String newRefreshToken) {
    if (newRefreshToken != null && !newRefreshToken.isBlank()) {
      return newRefreshToken;
    }
    return existing.refreshToken();
  }
}
