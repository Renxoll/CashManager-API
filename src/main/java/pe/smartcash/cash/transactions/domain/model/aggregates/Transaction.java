package pe.smartcash.cash.transactions.domain.model.aggregates;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import pe.smartcash.cash.transactions.domain.model.events.TransactionCategorized;
import pe.smartcash.cash.transactions.domain.model.valueobjects.CategoryCode;
import pe.smartcash.cash.transactions.domain.model.valueobjects.ExtractionSource;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Merchant;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Money;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionStatus;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

/**
 * Aggregate root del bounded context Transactions. El único punto de entrada para mutar su
 * estado son los métodos de comportamiento ({@link #categorize} / {@link #failExtraction}):
 * no hay setters, así que es imposible construir una transacción PROCESSED sin monto, o
 * volver a categorizar una que ya se resolvió.
 */
public final class Transaction {

  private final TransactionId id;
  private final UserId userId;
  private final String rawText;
  private final Instant createdAt;

  private TransactionStatus status;
  private Money money;
  private Merchant merchant;
  private CategoryCode categoryCode;
  private ExtractionSource extractionSource;
  private String errorMessage;
  private Instant processedAt;

  private final List<Object> domainEvents = new ArrayList<>();

  private Transaction(TransactionId id, UserId userId, String rawText, Instant createdAt, TransactionStatus status) {
    this.id = Objects.requireNonNull(id, "id");
    this.userId = Objects.requireNonNull(userId, "userId");
    if (rawText == null || rawText.isBlank()) {
      throw new IllegalArgumentException("rawText no puede estar vacío");
    }
    this.rawText = rawText;
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.status = status;
  }

  /** Punto de entrada al recibir un webhook nuevo: siempre nace PENDING. */
  public static Transaction receive(TransactionId id, UserId userId, String rawText, Instant receivedAt) {
    return new Transaction(id, userId, rawText, receivedAt, TransactionStatus.PENDING);
  }

  /** Reconstrucción desde persistencia: restaura estado sin re-aplicar invariantes de creación. */
  public static Transaction rehydrate(
      TransactionId id,
      UserId userId,
      String rawText,
      Instant createdAt,
      TransactionStatus status,
      Money money,
      Merchant merchant,
      CategoryCode categoryCode,
      ExtractionSource extractionSource,
      String errorMessage,
      Instant processedAt) {
    Transaction transaction = new Transaction(id, userId, rawText, createdAt, status);
    transaction.money = money;
    transaction.merchant = merchant;
    transaction.categoryCode = categoryCode;
    transaction.extractionSource = extractionSource;
    transaction.errorMessage = errorMessage;
    transaction.processedAt = processedAt;
    return transaction;
  }

  public void categorize(Money money, Merchant merchant, CategoryCode categoryCode, ExtractionSource source, Instant processedAt) {
    requireStatus(TransactionStatus.PENDING, "categorizar");
    this.money = Objects.requireNonNull(money, "money");
    this.merchant = Objects.requireNonNull(merchant, "merchant");
    this.categoryCode = Objects.requireNonNull(categoryCode, "categoryCode");
    this.extractionSource = Objects.requireNonNull(source, "extractionSource");
    this.processedAt = Objects.requireNonNull(processedAt, "processedAt");
    this.status = TransactionStatus.PROCESSED;
    this.domainEvents.add(new TransactionCategorized(id, userId, money, merchant, categoryCode, processedAt));
  }

  public void failExtraction(String reason) {
    requireStatus(TransactionStatus.PENDING, "marcar como fallida");
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("reason no puede estar vacío");
    }
    this.errorMessage = reason;
    this.status = TransactionStatus.FAILED;
  }

  /**
   * Reproceso de una transacción que había quedado FAILED: mismo resultado final que
   * {@link #categorize}, pero exige el estado previo contrario (FAILED, no PENDING) — una
   * transacción PROCESSED no se reprocesa, y una PENDING todavía no terminó su primer
   * intento. Limpia el {@code errorMessage} del intento fallido anterior.
   */
  public void retryExtraction(Money money, Merchant merchant, CategoryCode categoryCode, ExtractionSource source, Instant processedAt) {
    requireStatus(TransactionStatus.FAILED, "reprocesar");
    this.money = Objects.requireNonNull(money, "money");
    this.merchant = Objects.requireNonNull(merchant, "merchant");
    this.categoryCode = Objects.requireNonNull(categoryCode, "categoryCode");
    this.extractionSource = Objects.requireNonNull(source, "extractionSource");
    this.processedAt = Objects.requireNonNull(processedAt, "processedAt");
    this.errorMessage = null;
    this.status = TransactionStatus.PROCESSED;
    this.domainEvents.add(new TransactionCategorized(id, userId, money, merchant, categoryCode, processedAt));
  }

  private void requireStatus(TransactionStatus expected, String action) {
    if (this.status != expected) {
      throw new IllegalStateException(
          "No se puede %s una transacción en estado %s (se esperaba %s)".formatted(action, status, expected));
    }
  }

  /** Drena y limpia los eventos acumulados; la capa de aplicación los despacha tras persistir. */
  public List<Object> pullDomainEvents() {
    List<Object> events = List.copyOf(domainEvents);
    domainEvents.clear();
    return events;
  }

  public TransactionId id() {
    return id;
  }

  public UserId userId() {
    return userId;
  }

  public String rawText() {
    return rawText;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public TransactionStatus status() {
    return status;
  }

  public Money money() {
    return money;
  }

  public Merchant merchant() {
    return merchant;
  }

  public CategoryCode categoryCode() {
    return categoryCode;
  }

  public ExtractionSource extractionSource() {
    return extractionSource;
  }

  public String errorMessage() {
    return errorMessage;
  }

  public Instant processedAt() {
    return processedAt;
  }
}
