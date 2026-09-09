package pe.smartcash.cash.groups.infrastructure.persistence.jpa.repositories;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import pe.smartcash.cash.groups.infrastructure.persistence.ExpenseShareJpaEntity;

public interface ExpenseShareJpaRepository extends JpaRepository<ExpenseShareJpaEntity, UUID> {

  List<ExpenseShareJpaEntity> findAllByExpenseId(UUID expenseId);

  /** Carga en lote los shares de varios gastos a la vez -- evita N+1 al listar los gastos
   * de un grupo (ver SharedExpenseRepositoryAdapter.findAllByGroupId). */
  List<ExpenseShareJpaEntity> findAllByExpenseIdIn(Collection<UUID> expenseIds);

  /**
   * Reemplaza los shares en cada {@code save} del gasto (alta = no-op, edición = limpia los
   * viejos antes de reinsertar los recalculados). Bulk DELETE JPQL a propósito: ejecuta el
   * SQL de una, así los shares nuevos ({@code saveAll} posterior) no chocan con el índice
   * único {@code (expense_id, user_id)} contra las filas viejas todavía pendientes.
   */
  @Modifying
  @Query("delete from ExpenseShareJpaEntity s where s.expenseId = :expenseId")
  void deleteByExpenseId(UUID expenseId);

  /** Borra los shares de varios gastos a la vez -- parte del borrado en cascada del grupo. */
  @Modifying
  @Query("delete from ExpenseShareJpaEntity s where s.expenseId in :expenseIds")
  void deleteByExpenseIdIn(Collection<UUID> expenseIds);
}
