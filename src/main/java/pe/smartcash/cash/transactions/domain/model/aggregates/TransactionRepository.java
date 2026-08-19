package pe.smartcash.cash.transactions.domain.model.aggregates;

import java.util.List;
import java.util.Optional;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionStatus;

public interface TransactionRepository {

  void save(Transaction transaction);

  Optional<Transaction> findById(TransactionId id);

  List<Transaction> findAllByStatus(TransactionStatus status);
}
