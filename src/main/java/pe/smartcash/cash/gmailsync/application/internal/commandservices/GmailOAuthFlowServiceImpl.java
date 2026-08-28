package pe.smartcash.cash.gmailsync.application.internal.commandservices;

import org.springframework.stereotype.Service;
import pe.smartcash.cash.gmailsync.domain.exception.InvalidOAuthStateException;
import pe.smartcash.cash.gmailsync.domain.model.commands.StoreGmailConnectionCommand;
import pe.smartcash.cash.gmailsync.domain.services.GmailConnectionCommandService;
import pe.smartcash.cash.gmailsync.domain.services.GmailOAuthFlowService;
import pe.smartcash.cash.gmailsync.domain.services.GoogleOAuthPort;
import pe.smartcash.cash.gmailsync.domain.services.OAuthTokens;
import pe.smartcash.cash.gmailsync.infrastructure.oauth.OAuthStateStore;

@Service
class GmailOAuthFlowServiceImpl implements GmailOAuthFlowService {

  private final GoogleOAuthPort oauthPort;
  private final OAuthStateStore stateStore;
  private final GmailConnectionCommandService connectionCommandService;

  GmailOAuthFlowServiceImpl(GoogleOAuthPort oauthPort, OAuthStateStore stateStore, GmailConnectionCommandService connectionCommandService) {
    this.oauthPort = oauthPort;
    this.stateStore = stateStore;
    this.connectionCommandService = connectionCommandService;
  }

  @Override
  public String initiateConnection(String userId) {
    String state = stateStore.issue(userId);
    return oauthPort.buildAuthorizationUrl(state);
  }

  @Override
  public void completeConnection(String state, String authorizationCode) {
    String userId = stateStore.redeem(state).orElseThrow(InvalidOAuthStateException::new);
    OAuthTokens tokens = oauthPort.exchangeCode(authorizationCode);
    // fetchEmail nunca lanza (ver su contrato en GoogleOAuthPort) -- un email null no debe
    // frenar la conexión, solo degrada el panel de cuentas a "sin verificar".
    String email = oauthPort.fetchEmail(tokens.accessToken());
    connectionCommandService.handle(
        new StoreGmailConnectionCommand(userId, email, tokens.accessToken(), tokens.refreshToken(), tokens.accessTokenExpiresAt()));
  }
}
