package pe.smartcash.cash.transactions.interfaces.rest;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.smartcash.cash.transactions.domain.exception.TransactionNotFoundException;
import pe.smartcash.cash.transactions.domain.model.commands.MoveTransactionToWorkspaceCommand;
import pe.smartcash.cash.transactions.domain.model.commands.RecordManualIncomeCommand;
import pe.smartcash.cash.transactions.domain.model.commands.SetInternalTransferCommand;
import pe.smartcash.cash.transactions.domain.model.commands.UpdateTransactionCategoryCommand;
import pe.smartcash.cash.transactions.domain.model.queries.FindTransactionByIdQuery;
import pe.smartcash.cash.transactions.domain.model.queries.FindTransactionsByUserQuery;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;
import pe.smartcash.cash.transactions.domain.services.TransactionCommandService;
import pe.smartcash.cash.transactions.domain.services.TransactionDetail;
import pe.smartcash.cash.transactions.domain.services.TransactionQueryService;
import pe.smartcash.cash.transactions.interfaces.rest.resources.CategoryResource;
import pe.smartcash.cash.transactions.interfaces.rest.resources.MoveTransactionResource;
import pe.smartcash.cash.transactions.interfaces.rest.resources.RecordManualIncomeResource;
import pe.smartcash.cash.transactions.interfaces.rest.resources.SetInternalTransferResource;
import pe.smartcash.cash.transactions.interfaces.rest.resources.TransactionPageResource;
import pe.smartcash.cash.transactions.interfaces.rest.resources.TransactionResource;
import pe.smartcash.cash.transactions.interfaces.rest.resources.UpdateTransactionCategoryResource;
import pe.smartcash.cash.transactions.interfaces.rest.transform.TransactionResourceFromEntityAssembler;

/**
 * Endpoints autenticados para que el dueño de las transacciones las consulte y corrija --
 * distinto de {@code TransactionWebhookController} (ingesta) y {@code
 * SendGridInboundWebhookController}/{@code TransactionAdminController} (llamados por sistemas,
 * no por el usuario final).
 */
@RestController
@RequestMapping("/api/v1/transactions")
class TransactionController {

  private static final int MAX_PAGE_SIZE = 100;
  private static final int DEFAULT_PAGE_SIZE = 20;

  private final TransactionCommandService transactionCommandService;
  private final TransactionQueryService transactionQueryService;

  TransactionController(TransactionCommandService transactionCommandService, TransactionQueryService transactionQueryService) {
    this.transactionCommandService = transactionCommandService;
    this.transactionQueryService = transactionQueryService;
  }

  @GetMapping("/{transactionId}")
  ResponseEntity<TransactionResource> getById(@PathVariable UUID transactionId, @AuthenticationPrincipal String authenticatedUserId) {
    TransactionDetail detail = requireOwnedTransaction(TransactionId.of(transactionId), authenticatedUserId);
    return ResponseEntity.ok(TransactionResourceFromEntityAssembler.toResourceFromEntity(detail));
  }

  @GetMapping
  ResponseEntity<TransactionPageResource> list(
      @AuthenticationPrincipal String authenticatedUserId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
      @RequestParam(required = false) UUID workspaceId) {
    int clampedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
    int clampedPage = Math.max(0, page);
    var result =
        transactionQueryService.handle(
            new FindTransactionsByUserQuery(UserId.parse(authenticatedUserId), clampedPage, clampedSize, workspaceId));
    var items = result.items().stream().map(TransactionResourceFromEntityAssembler::toResourceFromEntity).toList();
    return ResponseEntity.ok(new TransactionPageResource(items, result.page(), result.size(), result.totalElements()));
  }

  @GetMapping("/categories")
  ResponseEntity<List<CategoryResource>> listCategories() {
    var resources =
        transactionQueryService.listCategories().stream()
            .map(descriptor -> new CategoryResource(descriptor.code().name(), descriptor.displayName(), descriptor.icon()))
            .toList();
    return ResponseEntity.ok(resources);
  }

  @PatchMapping("/{transactionId}/category")
  ResponseEntity<TransactionResource> updateCategory(
      @PathVariable UUID transactionId,
      @AuthenticationPrincipal String authenticatedUserId,
      @Valid @RequestBody UpdateTransactionCategoryResource resource) {
    TransactionId id = TransactionId.of(transactionId);
    UserId userId = UserId.parse(authenticatedUserId);
    // El code llega crudo: el caso de uso lo valida contra el catálogo cerrado (módulo
    // General) o contra las categorías del módulo custom, según dónde viva la transacción.
    transactionCommandService.handle(new UpdateTransactionCategoryCommand(id, userId, resource.categoryCode()));
    TransactionDetail detail = requireOwnedTransaction(id, authenticatedUserId);
    return ResponseEntity.ok(TransactionResourceFromEntityAssembler.toResourceFromEntity(detail));
  }

  /** Mueve la transacción a otro módulo del usuario. Para un gasto, {@code categoryCode} es
   * obligatorio y debe ser una categoría válida del módulo destino. */
  @PatchMapping("/{transactionId}/workspace")
  ResponseEntity<TransactionResource> moveToWorkspace(
      @PathVariable UUID transactionId,
      @AuthenticationPrincipal String authenticatedUserId,
      @Valid @RequestBody MoveTransactionResource resource) {
    TransactionId id = TransactionId.of(transactionId);
    UserId userId = UserId.parse(authenticatedUserId);
    transactionCommandService.handle(
        new MoveTransactionToWorkspaceCommand(id, userId, resource.workspaceId(), resource.categoryCode()));
    TransactionDetail detail = requireOwnedTransaction(id, authenticatedUserId);
    return ResponseEntity.ok(TransactionResourceFromEntityAssembler.toResourceFromEntity(detail));
  }

  @PatchMapping("/{transactionId}/internal-transfer")
  ResponseEntity<TransactionResource> setInternalTransfer(
      @PathVariable UUID transactionId,
      @AuthenticationPrincipal String authenticatedUserId,
      @Valid @RequestBody SetInternalTransferResource resource) {
    TransactionId id = TransactionId.of(transactionId);
    UserId userId = UserId.parse(authenticatedUserId);
    transactionCommandService.handle(new SetInternalTransferCommand(id, userId, resource.internalTransfer()));
    TransactionDetail detail = requireOwnedTransaction(id, authenticatedUserId);
    return ResponseEntity.ok(TransactionResourceFromEntityAssembler.toResourceFromEntity(detail));
  }

  @PostMapping("/income")
  ResponseEntity<TransactionResource> recordManualIncome(
      @AuthenticationPrincipal String authenticatedUserId, @Valid @RequestBody RecordManualIncomeResource resource) {
    UserId userId = UserId.parse(authenticatedUserId);
    TransactionId id =
        transactionCommandService.handle(
            new RecordManualIncomeCommand(
                userId,
                resource.amount(),
                resource.currency().trim().toUpperCase(),
                resource.source().trim(),
                resource.workspaceId()));
    TransactionDetail detail = requireOwnedTransaction(id, authenticatedUserId);
    return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResourceFromEntityAssembler.toResourceFromEntity(detail));
  }

  private TransactionDetail requireOwnedTransaction(TransactionId transactionId, String authenticatedUserId) {
    UserId userId = UserId.parse(authenticatedUserId);
    return transactionQueryService
        .handle(new FindTransactionByIdQuery(transactionId))
        .filter(detail -> detail.userId().equals(userId))
        .orElseThrow(() -> new TransactionNotFoundException(transactionId));
  }
}
