package pe.smartcash.cash.transactions.domain.model.aggregates;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import pe.smartcash.cash.transactions.domain.model.events.TransactionCategorized;
import pe.smartcash.cash.transactions.domain.model.events.TransactionReceived;
import pe.smartcash.cash.transactions.domain.model.valueobjects.CategoryCode;
import pe.smartcash.cash.transactions.domain.model.valueobjects.ExtractionSource;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Merchant;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Money;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionStatus;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionType;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

/**
 * Aggregate root del bounded context Transactions. El único punto de entrada para mutar su
 * estado son los métodos de comportamiento ({@link #categorize} / {@link #failExtraction} /
 * {@link #recategorize}): no hay setters, así que es imposible construir una transacción
 * PROCESSED sin monto. {@link #recategorize} es la única excepción deliberada a "una
 * transacción resuelta no cambia" — permite que el usuario corrija la categoría que asignó
 * el LLM, sin tocar monto/comercio/estado.
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
  private TransactionType type;
  private ExtractionSource extractionSource;
  private String errorMessage;
  private Instant processedAt;
  /** Transferencia entre cuentas propias del usuario: no cuenta como gasto ni ingreso real,
   * analytics la excluye de los totales. La activa/desactiva el usuario a mano (ver {@link
   * #markAsInternalTransfer} / {@link #unmarkAsInternalTransfer}). */
  private boolean internalTransfer = false;

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

  /**
   * Punto de entrada al recibir un webhook nuevo: siempre nace PENDING y emite {@link
   * TransactionReceived} para que la categorización (que depende del LLM) se dispare async,
   * fuera del ciclo request/response.
   */
  public static Transaction receive(TransactionId id, UserId userId, String rawText, Instant receivedAt) {
    Transaction transaction = new Transaction(id, userId, rawText, receivedAt, TransactionStatus.PENDING);
    transaction.domainEvents.add(new TransactionReceived(id, userId, rawText, receivedAt));
    return transaction;
  }

  /**
   * Ingreso cargado a mano por el usuario desde la app -- no hay notificación bancaria que
   * extraer, así que nace directo en PROCESSED (sin pasar por PENDING/categorize). {@code
   * extractionSource} queda MANUAL para que quede claro en la fila que este dato no vino del
   * LLM ni del cache. No emite ningún evento de dominio -- mismo criterio que {@link
   * #recategorize}: es una acción síncrona que el usuario ve reflejada al toque en la propia
   * pantalla, no necesita disparar una notificación push sobre algo que él mismo acaba de
   * escribir.
   */
  public static Transaction recordManualIncome(TransactionId id, UserId userId, String rawText, Money money, Merchant source, Instant recordedAt) {
    Transaction transaction = new Transaction(id, userId, rawText, recordedAt, TransactionStatus.PROCESSED);
    transaction.money = Objects.requireNonNull(money, "money");
    transaction.merchant = Objects.requireNonNull(source, "source");
    transaction.type = TransactionType.INCOME;
    transaction.categoryCode = null;
    transaction.extractionSource = ExtractionSource.MANUAL;
    transaction.processedAt = recordedAt;
    return transaction;
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
      TransactionType type,
      ExtractionSource extractionSource,
      String errorMessage,
      Instant processedAt,
      boolean internalTransfer) {
    Transaction transaction = new Transaction(id, userId, rawText, createdAt, status);
    transaction.money = money;
    transaction.merchant = merchant;
    transaction.categoryCode = categoryCode;
    transaction.type = type;
    transaction.extractionSource = extractionSource;
    transaction.errorMessage = errorMessage;
    transaction.processedAt = processedAt;
    transaction.internalTransfer = internalTransfer;
    return transaction;
  }

  /**
   * {@code categoryCode} se fuerza a {@code null} cuando {@code type} es INCOME, sin importar
   * qué mande el caller -- hay un solo bucket "Ingreso" en v1 (sin subcategorías), así que un
   * ingreso categorizado no tiene sentido de negocio, no es solo "el caller se olvidó".
   */
  public void categorize(Money money, Merchant merchant, CategoryCode categoryCode, ExtractionSource source, Instant processedAt, TransactionType type) {
    requireStatus(TransactionStatus.PENDING, "categorizar");
    this.money = Objects.requireNonNull(money, "money");
    this.merchant = Objects.requireNonNull(merchant, "merchant");
    this.type = Objects.requireNonNull(type, "type");
    this.categoryCode = type == TransactionType.EXPENSE ? Objects.requireNonNull(categoryCode, "categoryCode") : null;
    this.extractionSource = Objects.requireNonNull(source, "extractionSource");
    this.processedAt = Objects.requireNonNull(processedAt, "processedAt");
    this.status = TransactionStatus.PROCESSED;
    this.domainEvents.add(new TransactionCategorized(id, userId, money, merchant, this.categoryCode, processedAt));
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
  public void retryExtraction(
      Money money, Merchant merchant, CategoryCode categoryCode, ExtractionSource source, Instant processedAt, TransactionType type) {
    requireStatus(TransactionStatus.FAILED, "reprocesar");
    this.money = Objects.requireNonNull(money, "money");
    this.merchant = Objects.requireNonNull(merchant, "merchant");
    this.type = Objects.requireNonNull(type, "type");
    this.categoryCode = type == TransactionType.EXPENSE ? Objects.requireNonNull(categoryCode, "categoryCode") : null;
    this.extractionSource = Objects.requireNonNull(source, "extractionSource");
    this.processedAt = Objects.requireNonNull(processedAt, "processedAt");
    this.errorMessage = null;
    this.status = TransactionStatus.PROCESSED;
    this.domainEvents.add(new TransactionCategorized(id, userId, money, merchant, this.categoryCode, processedAt));
  }

  /**
   * Corrección manual de categoría por el usuario, después de que el LLM ya categorizó. Solo
   * toca {@code categoryCode} -- monto, comercio y estado quedan intactos. No emite evento de
   * dominio: hoy no hay ningún listener que necesite reaccionar a una recategorización (el
   * resumen mensual en el contexto analytics lee la tabla vía join en cada request, así que
   * se refleja solo).
   */
  public void recategorize(CategoryCode newCategoryCode) {
    requireStatus(TransactionStatus.PROCESSED, "recategorizar");
    if (this.type != TransactionType.EXPENSE) {
      throw new IllegalStateException("No se puede recategorizar una transacción de tipo " + this.type + " (solo aplica a EXPENSE)");
    }
    this.categoryCode = Objects.requireNonNull(newCategoryCode, "newCategoryCode");
  }

  /**
   * El usuario marca esta transacción como un movimiento entre sus propias cuentas: sigue
   * existiendo en su historial, pero analytics deja de contarla como gasto o ingreso. Solo
   * aplica sobre una transacción ya resuelta (con monto) -- a diferencia de {@link
   * #recategorize}, vale tanto para EXPENSE como para INCOME (los dos lados de un traspaso).
   * No emite evento de dominio: es una corrección síncrona que el usuario ve al toque, igual
   * que {@link #recategorize}.
   */
  public void markAsInternalTransfer() {
    requireStatus(TransactionStatus.PROCESSED, "marcar como transferencia propia");
    this.internalTransfer = true;
  }

  /** Revierte {@link #markAsInternalTransfer}: la transacción vuelve a contar en los totales. */
  public void unmarkAsInternalTransfer() {
    requireStatus(TransactionStatus.PROCESSED, "desmarcar como transferencia propia");
    this.internalTransfer = false;
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

  public TransactionType type() {
    return type;
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

  public boolean internalTransfer() {
    return internalTransfer;
  }
}
