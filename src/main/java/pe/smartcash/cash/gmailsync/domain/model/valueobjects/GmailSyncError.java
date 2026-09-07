package pe.smartcash.cash.gmailsync.domain.model.valueobjects;

/**
 * Motivo por el que una conexión dejó de poder sincronizar y necesita intervención del
 * usuario (no un fallo transitorio). Hoy hay un solo caso; es un enum para poder crecer.
 */
public enum GmailSyncError {

  /** El grant de Google ya no sirve: revocado por el usuario, o sin el scope gmail.readonly. */
  NEEDS_RECONNECT
}
