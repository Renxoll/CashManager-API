package pe.smartcash.cash.transactions.domain.model.aggregates;

import java.util.List;
import java.util.Optional;
import pe.smartcash.cash.transactions.domain.model.valueobjects.PendingSenderId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

public interface PendingSenderRepository {

  void save(PendingSender pendingSender);

  Optional<PendingSender> findById(PendingSenderId id);

  Optional<PendingSender> findByUserIdAndDomain(UserId userId, String domain);

  List<PendingSender> findAllPendingByUserId(UserId userId);
}
