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

class TransactionTest {

  private final UserId userId = UserId.of(UUID.randomUUID());

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
    Transaction transaction = Transaction.receive(TransactionId.newId(), userId, "S/10 en algún lado", Instant.now());

    assertThatThrownBy(() -> transaction.recategorize(CategoryCode.COMIDA)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void shouldRejectRecategorizingAFailedTransaction() {
    Transaction transaction = Transaction.receive(TransactionId.newId(), userId, "texto sin monto", Instant.now());
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
    Transaction transaction = Transaction.receive(TransactionId.newId(), userId, "Se abonó S/1500.00 a tu cuenta", Instant.now());

    // Le paso una categoría igual a propósito -- el agregado debe ignorarla, no confiar en
    // que el caller nunca la mande para un ingreso.
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
    Transaction transaction = Transaction.receive(TransactionId.newId(), userId, "Se abonó S/500.00 a tu cuenta", Instant.now());
    transaction.categorize(
        new Money(new BigDecimal("500.00"), "PEN"), new Merchant("Yo mismo"), null, ExtractionSource.LLM, Instant.now(), TransactionType.INCOME);

    transaction.markAsInternalTransfer();

    assertThat(transaction.internalTransfer()).isTrue();
  }

  @Test
  void shouldRejectMarkingAPendingTransactionAsInternalTransfer() {
    Transaction transaction = Transaction.receive(TransactionId.newId(), userId, "S/10 en algún lado", Instant.now());

    assertThatThrownBy(transaction::markAsInternalTransfer).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void shouldRejectRecategorizingAnIncomeTransaction() {
    Transaction transaction = Transaction.receive(TransactionId.newId(), userId, "Se abonó S/1500.00 a tu cuenta", Instant.now());
    transaction.categorize(
        new Money(new BigDecimal("1500.00"), "PEN"), new Merchant("Juan Pérez"), null, ExtractionSource.LLM, Instant.now(), TransactionType.INCOME);

    assertThatThrownBy(() -> transaction.recategorize(CategoryCode.COMIDA)).isInstanceOf(IllegalStateException.class);
  }

  private Transaction processedTransaction(CategoryCode categoryCode) {
    Transaction transaction = Transaction.receive(TransactionId.newId(), userId, "S/24.50 en Starbucks", Instant.now());
    transaction.categorize(
        new Money(new BigDecimal("24.50"), "PEN"), new Merchant("Starbucks"), categoryCode, ExtractionSource.LLM, Instant.now(), TransactionType.EXPENSE);
    return transaction;
  }
}
