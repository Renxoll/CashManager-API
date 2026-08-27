package pe.smartcash.cash.transactions.interfaces.rest;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.smartcash.cash.transactions.domain.model.commands.ApprovePendingSenderCommand;
import pe.smartcash.cash.transactions.domain.model.commands.RejectPendingSenderCommand;
import pe.smartcash.cash.transactions.domain.model.queries.FindPendingSendersByUserQuery;
import pe.smartcash.cash.transactions.domain.model.valueobjects.PendingSenderId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;
import pe.smartcash.cash.transactions.domain.services.PendingSenderCommandService;
import pe.smartcash.cash.transactions.domain.services.PendingSenderQueryService;
import pe.smartcash.cash.transactions.interfaces.rest.resources.PendingSenderResource;

/**
 * Remitentes que le llegaron al usuario pero no están en su lista de confianza -- en vez de
 * descartarlos en silencio (comportamiento anterior), quedan acá para que el usuario decida.
 */
@RestController
@RequestMapping("/api/v1/pending-senders")
class PendingSenderController {

  private final PendingSenderCommandService pendingSenderCommandService;
  private final PendingSenderQueryService pendingSenderQueryService;

  PendingSenderController(PendingSenderCommandService pendingSenderCommandService, PendingSenderQueryService pendingSenderQueryService) {
    this.pendingSenderCommandService = pendingSenderCommandService;
    this.pendingSenderQueryService = pendingSenderQueryService;
  }

  @GetMapping
  ResponseEntity<List<PendingSenderResource>> list(@AuthenticationPrincipal String authenticatedUserId) {
    var resources =
        pendingSenderQueryService.handle(new FindPendingSendersByUserQuery(UserId.parse(authenticatedUserId))).stream()
            .map(
                detail ->
                    new PendingSenderResource(
                        detail.id().value(),
                        detail.fromAddress(),
                        detail.domain(),
                        detail.sampleSnippet(),
                        detail.occurrenceCount(),
                        detail.firstSeenAt(),
                        detail.lastSeenAt()))
            .toList();
    return ResponseEntity.ok(resources);
  }

  @PostMapping("/{pendingSenderId}/approve")
  ResponseEntity<Void> approve(@PathVariable UUID pendingSenderId, @AuthenticationPrincipal String authenticatedUserId) {
    pendingSenderCommandService.handle(
        new ApprovePendingSenderCommand(PendingSenderId.of(pendingSenderId), UserId.parse(authenticatedUserId)));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{pendingSenderId}/reject")
  ResponseEntity<Void> reject(@PathVariable UUID pendingSenderId, @AuthenticationPrincipal String authenticatedUserId) {
    pendingSenderCommandService.handle(
        new RejectPendingSenderCommand(PendingSenderId.of(pendingSenderId), UserId.parse(authenticatedUserId)));
    return ResponseEntity.noContent().build();
  }
}
