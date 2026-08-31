package pe.smartcash.cash.gmailsync.domain.services;

/**
 * Resultado de una corrida de sincronización de Gmail (de un usuario o de todas las
 * conexiones). {@code connectionsSynced} cuenta solo las que terminaron sin excepción --
 * una conexión que falló (token revocado, Gmail caído) no frena a las demás pero tampoco
 * suma acá. Ver {@link GmailSyncService}.
 */
public record GmailSyncResult(int connectionsSynced, int transactionsIngested, int pendingSendersRegistered) {}
