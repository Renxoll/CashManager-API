package pe.smartcash.cash.gmailsync.application.internal;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pe.smartcash.cash.gmailsync.domain.model.aggregates.GmailConnection;
import pe.smartcash.cash.gmailsync.domain.model.aggregates.GmailConnectionRepository;
import pe.smartcash.cash.gmailsync.domain.services.BankNotificationIngestionPort;
import pe.smartcash.cash.gmailsync.domain.services.GmailMessage;
import pe.smartcash.cash.gmailsync.domain.services.GmailMessagePort;
import pe.smartcash.cash.gmailsync.domain.services.GoogleOAuthPort;
import pe.smartcash.cash.gmailsync.domain.services.OAuthTokens;

/**
 * Corre cada {@code app.gmail-sync.poll-interval-ms} (default 5 min): por cada usuario
 * conectado, refresca el access token si está por vencer, busca correos nuevos de los
 * remitentes confiables, y los ingesta por el mismo pipeline que el webhook de SendGrid.
 * Un usuario que falla (token revocado, Gmail caído, lo que sea) no debe frenar a los
 * demás -- cada conexión se procesa en su propio try/catch.
 */
@Slf4j
@Component
class GmailPollingJob {

  private final GmailConnectionRepository connectionRepository;
  private final GoogleOAuthPort oauthPort;
  private final GmailMessagePort gmailMessagePort;
  private final BankNotificationIngestionPort ingestionPort;
  private final Clock clock;
  private final Set<String> trustedSenderDomains;

  GmailPollingJob(
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

  @Scheduled(fixedDelayString = "${app.gmail-sync.poll-interval-ms:300000}")
  void pollAllConnections() {
    List<GmailConnection> connections = connectionRepository.findAll();
    for (GmailConnection connection : connections) {
      try {
        pollConnection(connection);
      } catch (Exception e) {
        log.warn("Fallo sincronizando Gmail para el usuario {}: {}", connection.userId().value(), e.getMessage(), e);
      }
    }
  }

  private void pollConnection(GmailConnection connection) {
    Instant now = clock.instant();
    if (connection.needsRefresh(now)) {
      OAuthTokens refreshed = oauthPort.refresh(connection.refreshToken());
      connection.refreshAccessToken(refreshed.accessToken(), refreshed.refreshToken(), refreshed.accessTokenExpiresAt(), now);
      connectionRepository.save(connection);
    }

    String userId = connection.userId().value().toString();

    List<GmailMessage> trustedMessages =
        gmailMessagePort.findMatchingMessagesSince(connection.accessToken(), connection.lastSyncedAt(), trustedSenderDomains);
    for (GmailMessage message : trustedMessages) {
      ingestMessage(userId, message);
    }

    // Búsqueda separada (excluye a nivel de query los dominios ya confiables, ver
    // GoogleGmailApiAdapter.buildCandidateQuery) para descubrir remitentes nuevos que la
    // búsqueda de arriba nunca trae -- están restringidos a from:<dominio confiable>, así
    // que un remitente nuevo ni siquiera se consulta ahí.
    List<GmailMessage> candidateMessages =
        gmailMessagePort.findCandidateMessagesSince(connection.accessToken(), connection.lastSyncedAt(), trustedSenderDomains);
    for (GmailMessage message : candidateMessages) {
      handleCandidateMessage(userId, message);
    }

    connection.recordSync(now);
    connectionRepository.save(connection);
  }

  private void ingestMessage(String userId, GmailMessage message) {
    // Defensa en profundidad: el "from:" de Gmail ya filtró por dominio en la búsqueda,
    // pero re-validar acá con la misma política que usa el webhook de SendGrid evita
    // confiar dos veces en la misma lógica implementada en dos lugares distintos.
    if (!ingestionPort.isTrustedSender(userId, message.from())) {
      log.info("Correo de Gmail descartado, remitente no confiable pese al filtro de búsqueda: {}", message.from());
      return;
    }
    try {
      ingestionPort.ingest(userId, message.rawText());
    } catch (Exception e) {
      log.warn("Fallo ingiriendo un correo de Gmail para el usuario {}: {}", userId, e.getMessage(), e);
    }
  }

  private void handleCandidateMessage(String userId, GmailMessage message) {
    // Re-chequea contra la política de confianza (no solo contra el allowlist global que ya
    // excluyó la query): si este usuario en particular ya aprobó el dominio, el mensaje se
    // ingesta directo en vez de volver a quedar pendiente en cada poll.
    if (ingestionPort.isTrustedSender(userId, message.from())) {
      ingestMessage(userId, message);
      return;
    }
    try {
      ingestionPort.recordPendingSender(userId, message.from(), message.rawText());
    } catch (Exception e) {
      log.warn("Fallo registrando remitente pendiente de Gmail para el usuario {}: {}", userId, e.getMessage(), e);
    }
  }
}
