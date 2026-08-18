package pe.smartcash.cash.transactions.infrastructure.persistence;

import pe.smartcash.cash.transactions.domain.model.valueobjects.CategoryCode;
import pe.smartcash.cash.transactions.domain.model.valueobjects.ExtractionSource;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Merchant;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Money;
import pe.smartcash.cash.transactions.domain.model.aggregates.Transaction;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

final class TransactionEntityMapper {

  private TransactionEntityMapper() {}

  static TransactionJpaEntity toJpaEntity(Transaction transaction, CategoryJpaEntity categoryEntity) {
    Money money = transaction.money();
    Merchant merchant = transaction.merchant();
    return TransactionJpaEntity.builder()
        .id(transaction.id().value())
        .userId(transaction.userId().value())
        .category(categoryEntity)
        .rawText(transaction.rawText())
        .amount(money != null ? money.amount() : null)
        .currency(money != null ? money.currency() : null)
        .merchant(merchant != null ? merchant.name() : null)
        .status(transaction.status())
        // La columna es NOT NULL; para transacciones FAILED el dominio no tiene una fuente
        // real (nunca llegó a extraerse), así que se persiste LLM como valor por defecto.
        // Es un detalle de la tabla, no una afirmación de negocio — por eso vive acá y no
        // en el agregado.
        .extractionSource(transaction.extractionSource() != null ? transaction.extractionSource() : ExtractionSource.LLM)
        .errorMessage(transaction.errorMessage())
        .createdAt(transaction.createdAt())
        .processedAt(transaction.processedAt())
        .build();
  }

  static Transaction toDomain(TransactionJpaEntity entity) {
    Money money = entity.getAmount() != null ? new Money(entity.getAmount(), entity.getCurrency()) : null;
    Merchant merchant = entity.getMerchant() != null ? new Merchant(entity.getMerchant()) : null;
    CategoryCode categoryCode = entity.getCategory() != null ? CategoryCode.fromCode(entity.getCategory().getCode()) : null;
    return Transaction.rehydrate(
        TransactionId.of(entity.getId()),
        UserId.of(entity.getUserId()),
        entity.getRawText(),
        entity.getCreatedAt(),
        entity.getStatus(),
        money,
        merchant,
        categoryCode,
        entity.getExtractionSource(),
        entity.getErrorMessage(),
        entity.getProcessedAt());
  }
}
