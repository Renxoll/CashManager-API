package pe.smartcash.cash.groups.domain.exception;

import pe.smartcash.cash.groups.domain.model.valueobjects.ExpenseId;

/** El gasto no existe, o existe pero no pertenece al grupo indicado en la ruta -- mismo
 * criterio "404 en ambos casos" que {@code GroupNotFoundException}. */
public class ExpenseNotFoundException extends RuntimeException {

  public ExpenseNotFoundException(ExpenseId expenseId) {
    super("Gasto no encontrado: " + expenseId.value());
  }
}
