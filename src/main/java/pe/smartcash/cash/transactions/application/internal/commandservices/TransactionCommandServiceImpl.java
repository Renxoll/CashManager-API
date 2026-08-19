package pe.smartcash.cash.transactions.application.internal.commandservices;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smartcash.cash.transactions.domain.exception.TransactionExtractionFailedException;
import pe.smartcash.cash.transactions.domain.exception.UserNotFoundException;
import pe.smartcash.cash.transactions.domain.model.aggregates.Transaction;
import pe.smartcash.cash.transactions.domain.model.aggregates.TransactionRepository;
import pe.smartcash.cash.transactions.domain.model.commands.IngestBankNotificationCommand;
import pe.smartcash.cash.transactions.domain.model.commands.RetryFailedTransactionsCommand;
import pe.smartcash.cash.transactions.domain.model.events.TransactionCategorized;
import pe.smartcash.cash.transactions.domain.model.valueobjects.CategoryCode;
import pe.smartcash.cash.transactions.domain.model.valueobjects.ExtractionSource;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Merchant;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Money;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionStatus;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;
import pe.smartcash.cash.transactions.domain.policy.CategorizedExpenseNotificationPolicy;
import pe.smartcash.cash.transactions.domain.service.BankNotificationHeuristicParser;
import pe.smartcash.cash.transactions.domain.service.ParsedHint;
import pe.smartcash.cash.transactions.domain.services.ExtractionResult;
import pe.smartcash.cash.transactions.domain.services.MerchantCategoryCache;
import pe.smartcash.cash.transactions.domain.services.RetryFailedTransactionsResult;
import pe.smartcash.cash.transactions.domain.services.TransactionCommandService;
import pe.smartcash.cash.transactions.domain.services.TransactionExtractionService;
import pe.smartcash.cash.transactions.domain.services.UserDirectory;

/**
 * Implementación del caso de uso de escritura: recibir el texto crudo de una notificación
 * bancaria, categorizarlo y dejarlo listo para el usuario. Orquesta puertos de dominio; toda
 * regla de negocio real vive en {@link Transaction} y en la política de notificación, no acá.
 */
@Service
class TransactionCommandServiceImpl implements TransactionCommandService {

  private final TransactionRepository transactionRepository;
  private final TransactionExtractionService extractionService;
  private final MerchantCategoryCache merchantCategoryCache;
  private final BankNotificationHeuristicParser heuristicParser;
  private final UserDirectory userDirectory;
  private final CategorizedExpenseNotificationPolicy notificationPolicy;
  private final Clock clock;

  TransactionCommandServiceImpl(
      TransactionRepository transactionRepository,
      TransactionExtractionService extractionService,
      MerchantCategoryCache merchantCategoryCache,
      BankNotificationHeuristicParser heuristicParser,
      UserDirectory userDirectory,
      CategorizedExpenseNotificationPolicy notificationPolicy,
      Clock clock) {
    this.transactionRepository = transactionRepository;
    this.extractionService = extractionService;
    this.merchantCategoryCache = merchantCategoryCache;
    this.heuristicParser = heuristicParser;
    this.userDirectory = userDirectory;
    this.notificationPolicy = notificationPolicy;
    this.clock = clock;
  }

  @Override
  @Transactional(noRollbackFor = TransactionExtractionFailedException.class)
  public TransactionId handle(IngestBankNotificationCommand command) {
    UserId userId = UserId.parse(command.userId());
    if (!userDirectory.exists(userId)) {
      throw new UserNotFoundException(userId);
    }

    Transaction transaction = Transaction.receive(TransactionId.newId(), userId, command.rawText(), clock.instant());

    Extraction extraction = resolveExtraction(command.rawText());

    if (extraction.failed()) {
      // Se persiste el FAILED (con el rawText intacto) ANTES de lanzar: noRollbackFor
      // evita que Spring deshaga este save al ver la excepción no controlada propagarse.
      // El caso de uso lanza para que el borde REST responda 422, no 2xx con un body que
      // el emisor del webhook tendría que inspeccionar para enterarse de que falló.
      transaction.failExtraction(extraction.errorMessage());
      transactionRepository.save(transaction);
      throw new TransactionExtractionFailedException(extraction.errorMessage());
    }

    transaction.categorize(extraction.money(), extraction.merchant(), extraction.categoryCode(), extraction.source(), clock.instant());
    transactionRepository.save(transaction);

    if (extraction.source() == ExtractionSource.LLM) {
      merchantCategoryCache.remember(extraction.merchant(), extraction.categoryCode());
    }

    // El agregado ya emitió el evento al categorizar; acá se despacha tras persistir.
    for (Object event : transaction.pullDomainEvents()) {
      if (event instanceof TransactionCategorized categorized) {
        notificationPolicy.enforce(categorized);
      }
    }

    return transaction.id();
  }

  @Override
  @Transactional
  public RetryFailedTransactionsResult handle(RetryFailedTransactionsCommand command) {
    List<Transaction> failedTransactions = transactionRepository.findAllByStatus(TransactionStatus.FAILED);
    int retried = 0;
    int stillFailed = 0;

    for (Transaction transaction : failedTransactions) {
      try {
        // Reprocesar va directo al LLM (no al parser heurístico ni al cache): si falló la
        // primera vez fue justamente porque el LLM no pudo, así que reintentar el mismo
        // camino es lo que tiene sentido de negocio acá.
        ExtractionResult result = extractionService.extract(transaction.rawText());
        transaction.retryExtraction(result.money(), result.merchant(), result.categoryCode(), ExtractionSource.LLM, clock.instant());
        transactionRepository.save(transaction);
        merchantCategoryCache.remember(result.merchant(), result.categoryCode());

        for (Object event : transaction.pullDomainEvents()) {
          if (event instanceof TransactionCategorized categorized) {
            notificationPolicy.enforce(categorized);
          }
        }
        retried++;
      } catch (TransactionExtractionFailedException e) {
        // Sigue FAILED: es un job por lotes, no un request esperando una respuesta puntual
        // -- que UNA transacción vuelva a fallar no debe abortar el resto del lote.
        stillFailed++;
      }
    }

    return new RetryFailedTransactionsResult(retried, stillFailed);
  }

  private Extraction resolveExtraction(String rawText) {
    Optional<ParsedHint> hint = heuristicParser.parse(rawText);
    if (hint.isPresent()) {
      Optional<CategoryCode> cached = merchantCategoryCache.findCategoryFor(hint.get().merchant());
      if (cached.isPresent()) {
        return Extraction.success(hint.get().money(), hint.get().merchant(), cached.get(), ExtractionSource.CACHE);
      }
    }
    try {
      ExtractionResult result = extractionService.extract(rawText);
      return Extraction.success(result.money(), result.merchant(), result.categoryCode(), ExtractionSource.LLM);
    } catch (TransactionExtractionFailedException e) {
      return Extraction.failure(e.getMessage());
    }
  }

  private record Extraction(Money money, Merchant merchant, CategoryCode categoryCode, ExtractionSource source, String errorMessage) {

    static Extraction success(Money money, Merchant merchant, CategoryCode categoryCode, ExtractionSource source) {
      return new Extraction(money, merchant, categoryCode, source, null);
    }

    static Extraction failure(String errorMessage) {
      return new Extraction(null, null, null, null, errorMessage);
    }

    boolean failed() {
      return money == null;
    }
  }
}
