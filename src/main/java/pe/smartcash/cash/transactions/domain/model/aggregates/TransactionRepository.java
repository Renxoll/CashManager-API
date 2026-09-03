package pe.smartcash.cash.transactions.domain.model.aggregates;

import java.util.List;
import java.util.Optional;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionStatus;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.WorkspaceId;

public interface TransactionRepository {

  void save(Transaction transaction);

  Optional<Transaction> findById(TransactionId id);

  List<Transaction> findAllByStatus(TransactionStatus status);

  /** Más recientes primero. {@code size} lo clampea el caller (ver {@code TransactionQueryServiceImpl}). */
  TransactionPage findAllByUserId(UserId userId, int page, int size);

  /** Igual que {@link #findAllByUserId} pero acotado a un módulo. */
  TransactionPage findAllByUserIdAndWorkspaceId(UserId userId, WorkspaceId workspaceId, int page, int size);
}
