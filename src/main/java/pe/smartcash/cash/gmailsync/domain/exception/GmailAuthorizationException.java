package pe.smartcash.cash.gmailsync.domain.exception;

/**
 * El acceso OAuth de una conexión de Gmail dejó de ser válido de forma no recuperable:
 * refresh token revocado ({@code invalid_grant}), o access token sin los scopes que la
 * Gmail API exige ({@code ACCESS_TOKEN_SCOPE_INSUFFICIENT} / 401). Reintentar no sirve --
 * lo dispara la capa de sincronización para marcar la conexión como "reconectar".
 */
public class GmailAuthorizationException extends RuntimeException {

  public GmailAuthorizationException(String message) {
    super(message);
  }

  public GmailAuthorizationException(String message, Throwable cause) {
    super(message, cause);
  }
}
