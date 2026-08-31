package pe.smartcash.cash.gmailsync.interfaces.rest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.smartcash.cash.gmailsync.domain.model.commands.DisconnectGmailConnectionCommand;
import pe.smartcash.cash.gmailsync.domain.model.queries.FindGmailConnectionsByUserQuery;
import pe.smartcash.cash.gmailsync.domain.model.valueobjects.GmailConnectionId;
import pe.smartcash.cash.gmailsync.domain.model.valueobjects.UserId;
import pe.smartcash.cash.gmailsync.domain.services.GmailConnectionCommandService;
import pe.smartcash.cash.gmailsync.domain.services.GmailConnectionQueryService;
import pe.smartcash.cash.gmailsync.domain.services.GmailSyncResult;
import pe.smartcash.cash.gmailsync.domain.services.GmailSyncService;
import pe.smartcash.cash.gmailsync.interfaces.rest.resources.GmailConnectionResource;
import pe.smartcash.cash.gmailsync.interfaces.rest.resources.GmailSyncResultResource;

/**
 * Gestión de las cuentas de Gmail ya conectadas -- separado de {@code GmailOAuthController}
 * porque ese controller es puramente del flujo OAuth (redirects), mientras que este es la
 * API normal autenticada que consume el panel de cuentas del frontend.
 */
@RestController
@RequestMapping("/api/v1/gmail/connections")
class GmailConnectionController {

  private final GmailConnectionCommandService commandService;
  private final GmailConnectionQueryService queryService;
  private final GmailSyncService syncService;

  GmailConnectionController(
      GmailConnectionCommandService commandService, GmailConnectionQueryService queryService, GmailSyncService syncService) {
    this.commandService = commandService;
    this.queryService = queryService;
    this.syncService = syncService;
  }

  @GetMapping
  ResponseEntity<List<GmailConnectionResource>> list(@AuthenticationPrincipal String authenticatedUserId) {
    var resources =
        queryService.handle(new FindGmailConnectionsByUserQuery(UserId.parse(authenticatedUserId))).stream()
            .map(detail -> new GmailConnectionResource(detail.id().value(), detail.email(), detail.connectedAt(), detail.lastSyncedAt()))
            .toList();
    return ResponseEntity.ok(resources);
  }

  /**
   * Refresco manual: sincroniza ahora las bandejas del usuario autenticado (el mismo trabajo
   * que el job programado, pero acotado a este usuario) y devuelve cuántos gastos entraron.
   * Pensado para el botón "refrescar gastos" del frontend; en un plan free donde el job
   * programado no corre mientras el servicio duerme, es también la vía normal de sincronizar.
   */
  @PostMapping("/sync")
  ResponseEntity<GmailSyncResultResource> sync(@AuthenticationPrincipal String authenticatedUserId) {
    GmailSyncResult result = syncService.syncUser(UserId.parse(authenticatedUserId));
    return ResponseEntity.ok(
        new GmailSyncResultResource(
            result.connectionsSynced(), result.transactionsIngested(), result.pendingSendersRegistered(), Instant.now()));
  }

  @DeleteMapping("/{connectionId}")
  ResponseEntity<Void> disconnect(@PathVariable UUID connectionId, @AuthenticationPrincipal String authenticatedUserId) {
    commandService.handle(
        new DisconnectGmailConnectionCommand(GmailConnectionId.of(connectionId), UserId.parse(authenticatedUserId)));
    return ResponseEntity.noContent().build();
  }
}
