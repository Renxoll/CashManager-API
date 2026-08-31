package pe.smartcash.cash.gmailsync.application.internal;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.smartcash.cash.gmailsync.domain.model.aggregates.GmailConnection;
import pe.smartcash.cash.gmailsync.domain.model.aggregates.GmailConnectionRepository;
import pe.smartcash.cash.gmailsync.domain.model.valueobjects.UserId;
import pe.smartcash.cash.gmailsync.domain.services.BankNotificationIngestionPort;
import pe.smartcash.cash.gmailsync.domain.services.GmailMessage;
import pe.smartcash.cash.gmailsync.domain.services.GmailMessagePort;
import pe.smartcash.cash.gmailsync.domain.services.GmailSyncResult;
import pe.smartcash.cash.gmailsync.domain.services.GmailSyncService;
import pe.smartcash.cash.gmailsync.domain.services.GoogleOAuthPort;
import pe.smartcash.cash.gmailsync.domain.services.OAuthTokens;

/**
 * Por cada conexión: refresca el access token si está por vencer, busca correos nuevos de
 * los remitentes confiables y los ingesta por el mismo pipeline que el webhook de SendGrid.
 * Un usuario que falla (token revocado, Gmail caído, lo que sea) no debe frenar a los demás
 * -- cada conexión se procesa en su propio try/catch y solo suma a {@code connectionsSynced}
 * si terminó sin excepción.
 */
@Slf4j
@Service
class GmailSyncServiceImpl implements GmailSyncService {

  private final GmailConnectionRepository connectionRepository;
  private final GoogleOAuthPort oauthPort;
  private final GmailMessagePort gmailMessagePort;
  private final BankNotificationIngestionPort ingestionPort;
  private final Clock clock;
  private final Set<String> trustedSenderDomains;

  GmailSyncServiceImpl(
      GmailConnectionRepository connectionRepository,
      GoogleOAuthPort oauthPort,
      GmailMessagePort gmailMessagePort,
      BankNotificationIngestionPort ingestionPort,
      Clock clock,
      @Value("#{'${app.inbound-email.trusted-sender-domains}'.split(',')}") Set<String> trustedSenderDomains) {
    this.connectionRepository = connectionRepository;
    this.oauthPort = oauthPort;
    this.gmailMessagePort = gmailMessagePort;
    this.ingestionPort = ingestionPort;
    this.clock = clock;
    this.trustedSenderDomains = trustedSenderDomains;
  }

  @Override
  public GmailSyncResult syncAll() {
    return sync(connectionRepository.findAll());
  }

  @Override
  public GmailSyncResult syncUser(UserId userId) {
    return sync(connectionRepository.findAllByUserId(userId));
  }

  private GmailSyncResult sync(List<GmailConnection> connections) {
    int connectionsSynced = 0;
    int ingested = 0;
    int pending = 0;
    for (GmailConnection connection : connections) {
      try {
        ConnectionOutcome outcome = pollConnection(connection);
        connectionsSynced++;
        ingested += outcome.ingested();
        pending += outcome.pending();
      } catch (Exception e) {
        log.warn("Fallo sincronizando Gmail para el usuario {}: {}", connection.userId().value(), e.getMessage(), e);
      }
    }
    return new GmailSyncResult(connectionsSynced, ingested, pending);
  }

  private ConnectionOutcome pollConnection(GmailConnection connection) {
    Instant now = clock.instant();
    if (connection.needsRefresh(now)) {
      OAuthTokens refreshed = oauthPort.refresh(connection.refreshToken());
      connection.refreshAccessToken(refreshed.accessToken(), refreshed.refreshToken(), refreshed.accessTokenExpiresAt(), now);
      connectionRepository.save(connection);
    }

    String userId = connection.userId().value().toString();
    Instant since = effectiveSince(connection.lastSyncedAt(), now);

    int ingested = 0;
    int pending = 0;

    List<GmailMessage> trustedMessages =
        gmailMessagePort.findMatchingMessagesSince(connection.accessToken(), since, trustedSenderDomains);
    for (GmailMessage message : trustedMessages) {
      ingested += ingestMessage(userId, message);
    }

    // Búsqueda separada (excluye a nivel de query los dominios ya confiables, ver
    // GoogleGmailApiAdapter.buildCandidateQuery) para descubrir remitentes nuevos que la
    // búsqueda de arriba nunca trae -- están restringidos a from:<dominio confiable>, así
    // que un remitente nuevo ni siquiera se consulta ahí.
    List<GmailMessage> candidateMessages =
        gmailMessagePort.findCandidateMessagesSince(connection.accessToken(), since, trustedSenderDomains);
    for (GmailMessage message : candidateMessages) {
      // Si este usuario en particular ya aprobó el dominio, se ingesta directo en vez de
      // volver a quedar pendiente en cada poll; si no, se registra como remitente pendiente.
      if (ingestionPort.isTrustedSender(userId, message.from())) {
        ingested += ingestMessage(userId, message);
      } else {
        pending += recordPendingSender(userId, message);
      }
    }

    connection.recordSync(now);
    connectionRepository.save(connection);
    return new ConnectionOutcome(ingested, pending);
  }

  /**
   * Nunca busca más atrás que el inicio del mes calendario actual (UTC), sin importar cuánto
   * tiempo llevaba {@code lastSyncedAt} sin correr -- una conexión reactivada tras meses
   * inactiva, o el primer sync de una conexión recién creada ({@code lastSyncedAt == null}),
   * no debe releer todo el historial de la bandeja.
   */
  static Instant effectiveSince(Instant lastSyncedAt, Instant now) {
    Instant startOfCurrentMonth = LocalDate.ofInstant(now, ZoneOffset.UTC).withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    if (lastSyncedAt == null || lastSyncedAt.isBefore(startOfCurrentMonth)) {
      return startOfCurrentMonth;
    }
    return lastSyncedAt;
  }

  /** @return 1 si el correo se ingestó, 0 si se descartó (remitente no confiable) o falló. */
  private int ingestMessage(String userId, GmailMessage message) {
    // Defensa en profundidad: el "from:" de Gmail ya filtró por dominio en la búsqueda,
    // pero re-validar acá con la misma política que usa el webhook de SendGrid evita
    // confiar dos veces en la misma lógica implementada en dos lugares distintos.
    if (!ingestionPort.isTrustedSender(userId, message.from())) {
      log.info("Correo de Gmail descartado, remitente no confiable pese al filtro de búsqueda: {}", message.from());
      return 0;
    }
    try {
      ingestionPort.ingest(userId, message.rawText());
      return 1;
    } catch (Exception e) {
      log.warn("Fallo ingiriendo un correo de Gmail para el usuario {}: {}", userId, e.getMessage(), e);
      return 0;
    }
  }

  /** @return 1 si quedó registrado como remitente pendiente, 0 si falló. */
  private int recordPendingSender(String userId, GmailMessage message) {
    try {
      ingestionPort.recordPendingSender(userId, message.from(), message.rawText());
      return 1;
    } catch (Exception e) {
      log.warn("Fallo registrando remitente pendiente de Gmail para el usuario {}: {}", userId, e.getMessage(), e);
      return 0;
    }
  }

  private record ConnectionOutcome(int ingested, int pending) {}
}
