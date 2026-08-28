package pe.smartcash.cash.transactions.interfaces.rest;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.smartcash.cash.transactions.domain.exception.TransactionNotFoundException;
import pe.smartcash.cash.transactions.domain.model.commands.UpdateTransactionCategoryCommand;
import pe.smartcash.cash.transactions.domain.model.queries.FindTransactionByIdQuery;
import pe.smartcash.cash.transactions.domain.model.queries.FindTransactionsByUserQuery;
import pe.smartcash.cash.transactions.domain.model.valueobjects.CategoryCode;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;
import pe.smartcash.cash.transactions.domain.services.TransactionCommandService;
import pe.smartcash.cash.transactions.domain.services.TransactionDetail;
import pe.smartcash.cash.transactions.domain.services.TransactionQueryService;
import pe.smartcash.cash.transactions.interfaces.rest.resources.CategoryResource;
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
      @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
    int clampedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
    int clampedPage = Math.max(0, page);
    var result =
        transactionQueryService.handle(new FindTransactionsByUserQuery(UserId.parse(authenticatedUserId), clampedPage, clampedSize));
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
    // valueOf estricto a propósito: a diferencia de CategoryCode.fromCode (que usa el
    // parseo tolerante del LLM y cae a OTROS ante cualquier basura), acá un código inválido
    // que mande el cliente es un error del caller y debe volver 400, no colarse como OTROS.
    CategoryCode categoryCode = CategoryCode.valueOf(resource.categoryCode().trim().toUpperCase());
    transactionCommandService.handle(new UpdateTransactionCategoryCommand(id, userId, categoryCode));
    TransactionDetail detail = requireOwnedTransaction(id, authenticatedUserId);
    return ResponseEntity.ok(TransactionResourceFromEntityAssembler.toResourceFromEntity(detail));
  }

  private TransactionDetail requireOwnedTransaction(TransactionId transactionId, String authenticatedUserId) {
    UserId userId = UserId.parse(authenticatedUserId);
    return transactionQueryService
        .handle(new FindTransactionByIdQuery(transactionId))
        .filter(detail -> detail.userId().equals(userId))
        .orElseThrow(() -> new TransactionNotFoundException(transactionId));
  }
}
