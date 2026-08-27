package pe.smartcash.cash.gmailsync.domain.services;

public interface GmailOAuthFlowService {

  /** @return la URL de consentimiento de Google a la que redirigir al usuario. */
  String initiateConnection(String userId);

  /** @throws pe.smartcash.cash.gmailsync.domain.exception.InvalidOAuthStateException si el
   * {@code state} no es válido (expiró, ya se usó, o no lo emitió este backend). */
  void completeConnection(String state, String authorizationCode);
}
