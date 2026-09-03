package pe.smartcash.cash.transactions.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.smartcash.cash.transactions.domain.model.valueobjects.CategoryCode;
import pe.smartcash.cash.transactions.domain.model.valueobjects.ExtractionSource;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Merchant;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Money;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionStatus;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionType;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.WorkspaceCategoryId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.WorkspaceId;

class TransactionTest {

  private final UserId userId = UserId.of(UUID.randomUUID());
  private final WorkspaceId generalWorkspace = WorkspaceId.of(UUID.randomUUID());
  private final WorkspaceId customWorkspace = WorkspaceId.of(UUID.randomUUID());

  private Transaction received(String rawText) {
    return Transaction.receive(TransactionId.newId(), userId, rawText, Instant.now(), generalWorkspace);
  }

  @Test
  void shouldRecategorizeAProcessedTransaction() {
    Transaction transaction = processedTransaction(CategoryCode.COMIDA);

    transaction.recategorize(CategoryCode.ENTRETENIMIENTO);

    assertThat(transaction.categoryCode()).isEqualTo(CategoryCode.ENTRETENIMIENTO);
    assertThat(transaction.status()).isEqualTo(TransactionStatus.PROCESSED);
  }

  @Test
  void shouldNotChangeAmountMerchantOrStatusWhenRecategorizing() {
    Transaction transaction = processedTransaction(CategoryCode.COMIDA);
    Money originalMoney = transaction.money();
    Merchant originalMerchant = transaction.merchant();

    transaction.recategorize(CategoryCode.SALUD);

    assertThat(transaction.money()).isEqualTo(originalMoney);
    assertThat(transaction.merchant()).isEqualTo(originalMerchant);
    assertThat(transaction.status()).isEqualTo(TransactionStatus.PROCESSED);
  }

  @Test
  void shouldRejectRecategorizingAPendingTransaction() {
    Transaction transaction = received("S/10 en algún lado");

    assertThatThrownBy(() -> transaction.recategorize(CategoryCode.COMIDA)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void shouldRejectRecategorizingAFailedTransaction() {
    Transaction transaction = received("texto sin monto");
    transaction.failExtraction("El LLM no devolvió un JSON válido");

    assertThatThrownBy(() -> transaction.recategorize(CategoryCode.COMIDA)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void shouldRejectNullCategory() {
    Transaction transaction = processedTransaction(CategoryCode.COMIDA);

    assertThatThrownBy(() -> transaction.recategorize(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldForceCategoryCodeToNullForIncome() {
    Transaction transaction = received("Se abonó S/1500.00 a tu cuenta");

    transaction.categorize(
        new Money(new BigDecimal("1500.00"), "PEN"),
        new Merchant("Juan Pérez"),
        CategoryCode.COMPRAS,
        ExtractionSource.LLM,
        Instant.now(),
        TransactionType.INCOME);

    assertThat(transaction.categoryCode()).isNull();
    assertThat(transaction.type()).isEqualTo(TransactionType.INCOME);
  }

  @Test
  void shouldMarkAndUnmarkAProcessedTransactionAsInternalTransfer() {
    Transaction transaction = processedTransaction(CategoryCode.COMIDA);
    assertThat(transaction.internalTransfer()).isFalse();

    transaction.markAsInternalTransfer();
    assertThat(transaction.internalTransfer()).isTrue();
    assertThat(transaction.categoryCode()).isEqualTo(CategoryCode.COMIDA);
    assertThat(transaction.status()).isEqualTo(TransactionStatus.PROCESSED);

    transaction.unmarkAsInternalTransfer();
    assertThat(transaction.internalTransfer()).isFalse();
  }

  @Test
  void shouldAllowMarkingAnIncomeAsInternalTransfer() {
    Transaction transaction = received("Se abonó S/500.00 a tu cuenta");
    transaction.categorize(
        new Money(new BigDecimal("500.00"), "PEN"), new Merchant("Yo mismo"), null, ExtractionSource.LLM, Instant.now(), TransactionType.INCOME);

    transaction.markAsInternalTransfer();

    assertThat(transaction.internalTransfer()).isTrue();
  }

  @Test
  void shouldRejectMarkingAPendingTransactionAsInternalTransfer() {
    Transaction transaction = received("S/10 en algún lado");

    assertThatThrownBy(transaction::markAsInternalTransfer).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void shouldRejectRecategorizingAnIncomeTransaction() {
    Transaction transaction = received("Se abonó S/1500.00 a tu cuenta");
    transaction.categorize(
        new Money(new BigDecimal("1500.00"), "PEN"), new Merchant("Juan Pérez"), null, ExtractionSource.LLM, Instant.now(), TransactionType.INCOME);

    assertThatThrownBy(() -> transaction.recategorize(CategoryCode.COMIDA)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void newTransactionStartsInTheGivenWorkspace() {
    Transaction transaction = received("S/10 en algún lado");

    assertThat(transaction.workspaceId()).isEqualTo(generalWorkspace);
    assertThat(transaction.workspaceCategoryId()).isNull();
  }

  @Test
  void movingAnExpenseToACustomWorkspaceSwapsTheCategoryToTheWorkspaceCategory() {
    Transaction transaction = processedTransaction(CategoryCode.COMIDA);
    WorkspaceCategoryId targetCategory = WorkspaceCategoryId.of(UUID.randomUUID());

    transaction.moveToWorkspace(customWorkspace, null, targetCategory);

    assertThat(transaction.workspaceId()).isEqualTo(customWorkspace);
    assertThat(transaction.workspaceCategoryId()).isEqualTo(targetCategory);
    assertThat(transaction.categoryCode()).isNull();
  }

  @Test
  void movingBackToTheGeneralWorkspaceRestoresACategoryCode() {
    Transaction transaction = processedTransaction(CategoryCode.COMIDA);
    transaction.moveToWorkspace(customWorkspace, null, WorkspaceCategoryId.of(UUID.randomUUID()));

    transaction.moveToWorkspace(generalWorkspace, CategoryCode.TRANSPORTE, null);

    assertThat(transaction.workspaceId()).isEqualTo(generalWorkspace);
    assertThat(transaction.categoryCode()).isEqualTo(CategoryCode.TRANSPORTE);
    assertThat(transaction.workspaceCategoryId()).isNull();
  }

  @Test
  void movingAnExpenseWithBothOrNeitherCategoryIsRejected() {
    Transaction transaction = processedTransaction(CategoryCode.COMIDA);

    assertThatThrownBy(() -> transaction.moveToWorkspace(customWorkspace, null, null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> transaction.moveToWorkspace(customWorkspace, CategoryCode.SALUD, WorkspaceCategoryId.of(UUID.randomUUID())))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void movingAPendingTransactionIsRejected() {
    Transaction transaction = received("S/10 en algún lado");

    assertThatThrownBy(() -> transaction.moveToWorkspace(customWorkspace, CategoryCode.COMIDA, null))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void recategorizeWithinSwapsToAWorkspaceCategory() {
    Transaction transaction = processedTransaction(CategoryCode.COMIDA);
    transaction.moveToWorkspace(customWorkspace, null, WorkspaceCategoryId.of(UUID.randomUUID()));
    WorkspaceCategoryId next = WorkspaceCategoryId.of(UUID.randomUUID());

    transaction.recategorizeWithin(next);

    assertThat(transaction.workspaceCategoryId()).isEqualTo(next);
    assertThat(transaction.categoryCode()).isNull();
  }

  private Transaction processedTransaction(CategoryCode categoryCode) {
    Transaction transaction = received("S/24.50 en Starbucks");
    transaction.categorize(
        new Money(new BigDecimal("24.50"), "PEN"), new Merchant("Starbucks"), categoryCode, ExtractionSource.LLM, Instant.now(), TransactionType.EXPENSE);
    return transaction;
  }
}
