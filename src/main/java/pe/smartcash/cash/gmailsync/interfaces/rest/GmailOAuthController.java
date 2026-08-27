package pe.smartcash.cash.gmailsync.interfaces.rest;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.smartcash.cash.gmailsync.domain.exception.InvalidOAuthStateException;
import pe.smartcash.cash.gmailsync.domain.services.GmailOAuthFlowService;
import pe.smartcash.cash.gmailsync.interfaces.rest.resources.AuthorizationUrlResource;

/**
 * {@code /authorize} exige el Bearer normal (así el backend sabe qué usuario está
 * conectando su Gmail); {@code /callback} es público a propósito -- Google redirige el
 * navegador directo ahí, sin poder mandar un header Authorization, así que el userId viaja
 * indirecto vía {@code state} (ver {@code OAuthStateStore}). {@code SecurityConfig} lo
 * permite explícitamente.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/gmail/oauth")
class GmailOAuthController {

  private final GmailOAuthFlowService oauthFlowService;
  private final String successRedirectUrl;
  private final String errorRedirectUrl;

  GmailOAuthController(
      GmailOAuthFlowService oauthFlowService,
      @Value("${app.gmail-sync.success-redirect-url}") String successRedirectUrl,
      @Value("${app.gmail-sync.error-redirect-url}") String errorRedirectUrl) {
    this.oauthFlowService = oauthFlowService;
    this.successRedirectUrl = successRedirectUrl;
    this.errorRedirectUrl = errorRedirectUrl;
  }

  @GetMapping("/authorize")
  ResponseEntity<AuthorizationUrlResource> authorize(@AuthenticationPrincipal String authenticatedUserId) {
    String url = oauthFlowService.initiateConnection(authenticatedUserId);
    return ResponseEntity.ok(new AuthorizationUrlResource(url));
  }

  /**
   * Nunca devuelve un error HTTP acá: quien "llama" este endpoint es el navegador del
   * usuario siguiendo una redirección de Google, no un cliente que vaya a leer un body de
   * error -- cualquier fallo se comunica redirigiendo al frontend con un query param, no
   * con un status 4xx/5xx que el usuario nunca vería.
   */
  @GetMapping("/callback")
  ResponseEntity<Void> callback(
      @RequestParam String state,
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String error) {
    if (error != null || code == null) {
      log.info("Callback OAuth de Gmail sin autorización (usuario canceló o Google devolvió error: {})", error);
      return redirectTo(errorRedirectUrl);
    }
    try {
      oauthFlowService.completeConnection(state, code);
      return redirectTo(successRedirectUrl);
    } catch (InvalidOAuthStateException e) {
      log.info("Callback OAuth de Gmail con state inválido o expirado");
      return redirectTo(errorRedirectUrl);
    } catch (Exception e) {
      log.warn("Fallo completando la conexión de Gmail: {}", e.getMessage(), e);
      return redirectTo(errorRedirectUrl);
    }
  }

  private ResponseEntity<Void> redirectTo(String url) {
    return ResponseEntity.status(302).location(URI.create(url)).build();
  }
}
