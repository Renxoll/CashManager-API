package pe.smartcash.cash.transactions.interfaces.rest;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pe.smartcash.cash.transactions.domain.model.commands.IngestBankNotificationCommand;
import pe.smartcash.cash.transactions.domain.model.queries.FindTransactionByIdQuery;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;
import pe.smartcash.cash.transactions.domain.services.TransactionCommandService;
import pe.smartcash.cash.transactions.domain.services.TransactionDetail;
import pe.smartcash.cash.transactions.domain.services.TransactionQueryService;
import pe.smartcash.cash.transactions.interfaces.rest.resources.CreateTransactionResource;
import pe.smartcash.cash.transactions.interfaces.rest.resources.TransactionResource;
import pe.smartcash.cash.transactions.interfaces.rest.transform.CreateTransactionCommandFromResourceAssembler;
import pe.smartcash.cash.transactions.interfaces.rest.transform.TransactionResourceFromEntityAssembler;

@RestController
@RequestMapping("/api/v1/transactions")
class TransactionWebhookController {

  private final TransactionCommandService transactionCommandService;
  private final TransactionQueryService transactionQueryService;

  TransactionWebhookController(TransactionCommandService transactionCommandService, TransactionQueryService transactionQueryService) {
    this.transactionCommandService = transactionCommandService;
    this.transactionQueryService = transactionQueryService;
  }

  /**
   * {@code handle()} solo persiste PENDING y publica {@code TransactionReceived}: la
   * categorización real (llamada al LLM) corre en un worker async aparte (ver {@code
   * TransactionCommandServiceImpl}), así que este endpoint nunca espera al LLM y siempre
   * responde 202. El cliente hace poll de {@link #getById} (URL en {@code Location}) hasta
   * ver el estado PROCESSED o FAILED.
   */
  @PostMapping("/webhook")
  ResponseEntity<TransactionResource> receiveWebhook(@Valid @RequestBody CreateTransactionResource resource) {
    IngestBankNotificationCommand command = CreateTransactionCommandFromResourceAssembler.toCommandFromResource(resource);
    TransactionId transactionId = transactionCommandService.handle(command);

    TransactionDetail detail =
        transactionQueryService
            .handle(new FindTransactionByIdQuery(transactionId))
            .orElseThrow(() -> new IllegalStateException("La transacción recién creada no se encontró: " + transactionId));

    URI location =
        ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/v1/transactions/{id}")
            .buildAndExpand(transactionId.value())
            .toUri();

    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .location(location)
        .body(TransactionResourceFromEntityAssembler.toResourceFromEntity(detail));
  }

  @GetMapping("/{transactionId}")
  ResponseEntity<TransactionResource> getById(@PathVariable UUID transactionId) {
    return transactionQueryService
        .handle(new FindTransactionByIdQuery(TransactionId.of(transactionId)))
        .map(TransactionResourceFromEntityAssembler::toResourceFromEntity)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
