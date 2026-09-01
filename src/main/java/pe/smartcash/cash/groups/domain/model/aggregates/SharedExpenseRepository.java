package pe.smartcash.cash.groups.domain.model.aggregates;

import java.util.List;
import java.util.Optional;
import pe.smartcash.cash.groups.domain.model.valueobjects.ExpenseId;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;

public interface SharedExpenseRepository {

  /** Persiste el gasto y sus shares en la misma transacción -- ver el adaptador. */
  void save(SharedExpense expense);

  Optional<SharedExpense> findById(ExpenseId id);

  /** Más recientes primero. Sin paginado a propósito: a diferencia del historial bancario
   * de un usuario, la cantidad de gastos de un grupo puntual es chica. */
  List<SharedExpense> findAllByGroupId(GroupId groupId);
}
