package pe.smartcash.cash.gmailsync.domain.exception;

import pe.smartcash.cash.gmailsync.domain.model.valueobjects.GmailConnectionId;

/**
 * No existe, o existe pero no pertenece al usuario autenticado -- mismo 404 en ambos casos
 * (nunca 403), mismo criterio que las demás excepciones "not found" de dueño del proyecto.
 */
public class GmailConnectionNotFoundException extends RuntimeException {

  public GmailConnectionNotFoundException(GmailConnectionId connectionId) {
    super("Conexión de Gmail no encontrada: " + connectionId.value());
  }
}
