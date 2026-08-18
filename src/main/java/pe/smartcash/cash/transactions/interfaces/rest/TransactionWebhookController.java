package pe.smartcash.cash.transactions.interfaces.rest;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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

  @PostMapping("/webhook")
  ResponseEntity<TransactionResource> receiveWebhook(@Valid @RequestBody CreateTransactionResource resource) {
    IngestBankNotificationCommand command = CreateTransactionCommandFromResourceAssembler.toCommandFromResource(resource);
    // Si la extracción falla, handle() persiste la transacción como FAILED y lanza
    // TransactionExtractionFailedException; TransactionExceptionHandler la traduce a 422.
    // Llegar hasta acá implica que quedó PROCESSED.
    TransactionId transactionId = transactionCommandService.handle(command);

    TransactionDetail detail =
        transactionQueryService
            .handle(new FindTransactionByIdQuery(transactionId))
            .orElseThrow(() -> new IllegalStateException("La transacción recién creada no se encontró: " + transactionId));

    return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResourceFromEntityAssembler.toResourceFromEntity(detail));
  }
}
