package pe.smartcash.cash.gmailsync.domain.services;

import pe.smartcash.cash.gmailsync.domain.model.valueobjects.UserId;

/**
 * Sincroniza bandejas de Gmail conectadas: refresca el access token si hace falta, busca
 * correos nuevos de remitentes confiables y los ingesta por el mismo pipeline que el webhook
 * de SendGrid (ver {@code BankNotificationIngestionPort}). Dos entradas al mismo trabajo:
 * {@link #syncAll()} para el job programado ({@code GmailPollingJob}), {@link #syncUser(UserId)}
 * para el botón "refrescar gastos" del frontend ({@code GmailConnectionController}).
 */
public interface GmailSyncService {

  /** Todas las conexiones activas. Usado por el job programado. */
  GmailSyncResult syncAll();

  /** Solo las conexiones del usuario dado. Usado por el endpoint autenticado de refresco. */
  GmailSyncResult syncUser(UserId userId);
}
