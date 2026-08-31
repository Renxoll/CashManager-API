package pe.smartcash.cash.gmailsync.application.internal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pe.smartcash.cash.gmailsync.domain.services.GmailSyncResult;
import pe.smartcash.cash.gmailsync.domain.services.GmailSyncService;

/**
 * Dispara {@link GmailSyncService#syncAll()} cada {@code app.gmail-sync.poll-interval-ms}
 * (default 5 min). Toda la lógica de sincronización vive en el servicio -- acá solo está el
 * gatillo programado, porque el mismo trabajo se expone además como endpoint autenticado
 * para el botón "refrescar gastos" del frontend (ver {@code GmailConnectionController}).
 *
 * <p>En un plan sin instancia always-on (Render free duerme el servicio tras ~15 min
 * inactivo) este {@code @Scheduled} solo corre mientras algo mantiene el proceso despierto
 * -- un ping externo periódico, o el propio endpoint de refresco al abrir la app.
 */
@Slf4j
@Component
class GmailPollingJob {

  private final GmailSyncService gmailSyncService;

  GmailPollingJob(GmailSyncService gmailSyncService) {
    this.gmailSyncService = gmailSyncService;
  }

  @Scheduled(fixedDelayString = "${app.gmail-sync.poll-interval-ms:300000}")
  void pollAllConnections() {
    GmailSyncResult result = gmailSyncService.syncAll();
    if (result.transactionsIngested() > 0 || result.pendingSendersRegistered() > 0) {
      log.info(
          "Sync Gmail programado: {} conexiones, {} transacciones ingeridas, {} remitentes pendientes",
          result.connectionsSynced(),
          result.transactionsIngested(),
          result.pendingSendersRegistered());
    }
  }
}
