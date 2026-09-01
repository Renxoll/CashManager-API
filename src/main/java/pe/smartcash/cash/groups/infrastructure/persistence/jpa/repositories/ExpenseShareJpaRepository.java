package pe.smartcash.cash.groups.infrastructure.persistence.jpa.repositories;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.smartcash.cash.groups.infrastructure.persistence.ExpenseShareJpaEntity;

public interface ExpenseShareJpaRepository extends JpaRepository<ExpenseShareJpaEntity, UUID> {

  List<ExpenseShareJpaEntity> findAllByExpenseId(UUID expenseId);

  /** Carga en lote los shares de varios gastos a la vez -- evita N+1 al listar los gastos
   * de un grupo (ver SharedExpenseRepositoryAdapter.findAllByGroupId). */
  List<ExpenseShareJpaEntity> findAllByExpenseIdIn(Collection<UUID> expenseIds);
}
