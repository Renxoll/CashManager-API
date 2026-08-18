package pe.smartcash.cash.transactions.domain.policy;

import pe.smartcash.cash.transactions.domain.model.events.TransactionCategorized;

/**
 * Regla de negocio (política reactiva, en el sentido de Event Storming): el dueño de un
 * gasto recién categorizado recibe un resumen por push. Se nombra por la regla que
 * encierra, no por su mecánica ("cuando X entonces Y") — la mecánica vive en la
 * implementación, no en el nombre ni en un condicional expuesto.
 */
public interface CategorizedExpenseNotificationPolicy {

  void enforce(TransactionCategorized event);
}
