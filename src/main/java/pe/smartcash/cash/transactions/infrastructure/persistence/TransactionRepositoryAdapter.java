package pe.smartcash.cash.transactions.infrastructure.persistence;

import java.util.Optional;
import org.springframework.stereotype.Repository;
import pe.smartcash.cash.transactions.domain.model.aggregates.Transaction;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;
import pe.smartcash.cash.transactions.domain.model.aggregates.TransactionRepository;

@Repository
class TransactionRepositoryAdapter implements TransactionRepository {

  private final TransactionJpaRepository jpaRepository;
  private final CategoryJpaRepository categoryJpaRepository;

  TransactionRepositoryAdapter(TransactionJpaRepository jpaRepository, CategoryJpaRepository categoryJpaRepository) {
    this.jpaRepository = jpaRepository;
    this.categoryJpaRepository = categoryJpaRepository;
  }

  @Override
  public void save(Transaction transaction) {
    CategoryJpaEntity categoryEntity =
        transaction.categoryCode() != null
            ? categoryJpaRepository
                .findByCode(transaction.categoryCode().name())
                .orElseThrow(() -> new IllegalStateException("Categoría no encontrada en catálogo: " + transaction.categoryCode()))
            : null;
    jpaRepository.save(TransactionEntityMapper.toJpaEntity(transaction, categoryEntity));
  }

  @Override
  public Optional<Transaction> findById(TransactionId id) {
    return jpaRepository.findById(id.value()).map(TransactionEntityMapper::toDomain);
  }
}
