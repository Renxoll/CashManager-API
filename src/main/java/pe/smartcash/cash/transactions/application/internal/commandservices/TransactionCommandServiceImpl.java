package pe.smartcash.cash.transactions.application.internal.commandservices;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smartcash.cash.transactions.domain.exception.TransactionExtractionFailedException;
import pe.smartcash.cash.transactions.domain.exception.TransactionNotFoundException;
import pe.smartcash.cash.transactions.domain.exception.UserNotFoundException;
import pe.smartcash.cash.transactions.domain.model.aggregates.Transaction;
import pe.smartcash.cash.transactions.domain.model.aggregates.TransactionRepository;
import pe.smartcash.cash.transactions.domain.model.commands.IngestBankNotificationCommand;
import pe.smartcash.cash.transactions.domain.model.commands.IngestEmailedTransactionCommand;
import pe.smartcash.cash.transactions.domain.model.commands.RetryFailedTransactionsCommand;
import pe.smartcash.cash.transactions.domain.model.commands.UpdateTransactionCategoryCommand;
import pe.smartcash.cash.transactions.domain.model.events.TransactionCategorized;
import pe.smartcash.cash.transactions.domain.model.events.TransactionReceived;
import pe.smartcash.cash.transactions.domain.model.valueobjects.CategoryCode;
import pe.smartcash.cash.transactions.domain.model.valueobjects.ExtractionSource;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Merchant;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Money;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionStatus;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;
import pe.smartcash.cash.transactions.domain.policy.CategorizedExpenseNotificationPolicy;
import pe.smartcash.cash.transactions.domain.policy.TrustedBankSenderPolicy;
import pe.smartcash.cash.transactions.domain.service.BankNotificationHeuristicParser;
import pe.smartcash.cash.transactions.domain.service.ParsedHint;
import pe.smartcash.cash.transactions.domain.services.ExtractionResult;
import pe.smartcash.cash.transactions.domain.services.MerchantCategoryCache;
import pe.smartcash.cash.transactions.domain.services.PendingSenderCommandService;
import pe.smartcash.cash.transactions.domain.services.RetryFailedTransactionsResult;
import pe.smartcash.cash.transactions.domain.services.TransactionCommandService;
import pe.smartcash.cash.transactions.domain.services.TransactionExtractionService;
import pe.smartcash.cash.transactions.domain.services.UserDirectory;

/**
 * Implementación del caso de uso de escritura: recibir el texto crudo de una notificación
 * bancaria, categorizarlo y dejarlo listo para el usuario. Orquesta puertos de dominio; toda
 * regla de negocio real vive en {@link Transaction} y en la política de notificación, no acá.
 *
 * <p>La ingestión ({@code handle(IngestBankNotificationCommand)}) y la categorización
 * ({@code on(TransactionReceived)}) son dos casos de uso separados a propósito: el primero es
 * rápido y determinístico (solo persiste PENDING), el segundo depende del LLM (lento, puede
 * fallar) y corre async vía {@link ApplicationModuleListener} — ver {@code
 * TransactionWebhookController}, que responde 202 apenas termina el primero.
 */
@Slf4j
@Service
class TransactionCommandServiceImpl implements TransactionCommandService {

  private final TransactionRepository transactionRepository;
  private final TransactionExtractionService extractionService;
  private final MerchantCategoryCache merchantCategoryCache;
  private final BankNotificationHeuristicParser heuristicParser;
  private final UserDirectory userDirectory;
  private final CategorizedExpenseNotificationPolicy notificationPolicy;
  private final TrustedBankSenderPolicy trustedBankSenderPolicy;
  private final PendingSenderCommandService pendingSenderCommandService;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;

  TransactionCommandServiceImpl(
      TransactionRepository transactionRepository,
      TransactionExtractionService extractionService,
      MerchantCategoryCache merchantCategoryCache,
      BankNotificationHeuristicParser heuristicParser,
      UserDirectory userDirectory,
      CategorizedExpenseNotificationPolicy notificationPolicy,
      TrustedBankSenderPolicy trustedBankSenderPolicy,
      PendingSenderCommandService pendingSenderCommandService,
      ApplicationEventPublisher eventPublisher,
      Clock clock) {
    this.transactionRepository = transactionRepository;
    this.extractionService = extractionService;
    this.merchantCategoryCache = merchantCategoryCache;
    this.heuristicParser = heuristicParser;
    this.userDirectory = userDirectory;
    this.notificationPolicy = notificationPolicy;
    this.trustedBankSenderPolicy = trustedBankSenderPolicy;
    this.pendingSenderCommandService = pendingSenderCommandService;
    this.eventPublisher = eventPublisher;
    this.clock = clock;
  }

  @Override
  @Transactional
  public TransactionId handle(IngestBankNotificationCommand command) {
    UserId userId = UserId.parse(command.userId());
    if (!userDirectory.exists(userId)) {
      throw new UserNotFoundException(userId);
    }
    return ingest(userId, command.rawText());
  }

  /**
   * A diferencia del webhook JSON (donde un userId inexistente o inválido es un error del
   * caller que vale la pena reportar con 404), acá un buzón sin dueño no es un error del
   * sistema — es un reenvío mal dirigido, y nadie del otro lado de SendGrid puede "corregir"
   * el request — así que se descarta en silencio (log) en vez de lanzar; el endpoint igual
   * responde 200.
   *
   * <p>El buzón se resuelve ANTES de chequear el remitente (al revés que antes): un remitente
   * no confiable ya no se descarta en silencio, queda en la cola de aprobación del usuario
   * ({@link PendingSenderCommandService#recordSighting}) -- y para saber de qué usuario es esa
   * cola hace falta el userId, que solo se obtiene resolviendo el buzón primero.
   */
  @Override
  @Transactional
  public Optional<TransactionId> handle(IngestEmailedTransactionCommand command) {
    Optional<UserId> userId = userDirectory.findUserIdByInboxAddress(command.inboxAddress());
    if (userId.isEmpty()) {
      log.info("Correo entrante descartado, buzón sin dueño: {}", command.inboxAddress());
      return Optional.empty();
    }

    if (!trustedBankSenderPolicy.isSatisfiedBy(userId.get(), command.fromAddress())) {
      pendingSenderCommandService.recordSighting(userId.get(), command.fromAddress(), command.rawText());
      log.info("Correo entrante puesto en cola de aprobación, remitente no confiable: {}", command.fromAddress());
      return Optional.empty();
    }

    return Optional.of(ingest(userId.get(), command.rawText()));
  }

  private TransactionId ingest(UserId userId, String rawText) {
    Transaction transaction = Transaction.receive(TransactionId.newId(), userId, rawText, clock.instant());
    transactionRepository.save(transaction);

    // Publicar ANTES de que termine la transacción (no después): con
    // spring-modulith-starter-jpa, la publicación queda registrada en la tabla
    // event_publication en el mismo commit, así que si el proceso muere antes de que el
    // listener async corra, el evento no se pierde — queda pendiente para reintento.
    for (Object event : transaction.pullDomainEvents()) {
      if (event instanceof TransactionReceived received) {
        eventPublisher.publishEvent(received);
      }
    }

    return transaction.id();
  }

  /**
   * Worker asíncrono: {@code @ApplicationModuleListener} ya compone {@code @Async}, {@code
   * @TransactionalEventListener(phase = AFTER_COMMIT)} y {@code
   * @Transactional(propagation = REQUIRES_NEW)} -- por eso no lleva un {@code @Transactional}
   * propio, que pisaría esa propagación. Corre recién después del commit de {@code
   * handle(IngestBankNotificationCommand)}, en el {@code ThreadPoolTaskExecutor} de {@code
   * AsyncConfig}, nunca en el hilo del request HTTP.
   */
  @ApplicationModuleListener
  void on(TransactionReceived event) {
    Transaction transaction =
        transactionRepository
            .findById(event.transactionId())
            .orElseThrow(() -> new IllegalStateException("Transacción PENDING no encontrada: " + event.transactionId()));

    Extraction extraction = resolveExtraction(transaction.rawText());

    if (extraction.failed()) {
      // A diferencia del flujo síncrono anterior, acá nadie está esperando una respuesta
      // HTTP: se persiste FAILED y se corta, sin relanzar. El fallo de LLM ya quedó
      // reportado a Sentry en el punto donde se originó (OpenAiTransactionExtractionAdapter).
      transaction.failExtraction(extraction.errorMessage());
      transactionRepository.save(transaction);
      return;
    }

    transaction.categorize(extraction.money(), extraction.merchant(), extraction.categoryCode(), extraction.source(), clock.instant());
    transactionRepository.save(transaction);

    if (extraction.source() == ExtractionSource.LLM) {
      merchantCategoryCache.remember(extraction.merchant(), extraction.categoryCode());
    }

    for (Object domainEvent : transaction.pullDomainEvents()) {
      if (domainEvent instanceof TransactionCategorized categorized) {
        notificationPolicy.enforce(categorized);
      }
    }
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

  @Override
  @Transactional
  public void handle(UpdateTransactionCategoryCommand command) {
    Transaction transaction =
        transactionRepository
            .findById(command.transactionId())
            .filter(t -> t.userId().equals(command.requestingUserId()))
            .orElseThrow(() -> new TransactionNotFoundException(command.transactionId()));
    transaction.recategorize(command.newCategoryCode());
    transactionRepository.save(transaction);
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
