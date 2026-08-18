package pe.smartcash.cash.transactions.domain.model.aggregates;

import java.util.Optional;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;

public interface TransactionRepository {

  void save(Transaction transaction);

  Optional<Transaction> findById(TransactionId id);
}
