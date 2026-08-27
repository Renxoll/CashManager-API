package pe.smartcash.cash.transactions.interfaces.rest;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
   * responde 202. El cliente hace poll de {@code GET /api/v1/transactions/{id}} (URL en
   * {@code Location}, ahora en {@code TransactionController}) hasta ver el estado PROCESSED
   * o FAILED.
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
}
