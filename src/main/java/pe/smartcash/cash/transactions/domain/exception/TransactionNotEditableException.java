package pe.smartcash.cash.transactions.domain.exception;

import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;

/**
 * La transacción existe pero su estado no admite completar o corregir el gasto a mano: sigue
 * PENDING (el LLM aún no termina), es un ingreso o es una transferencia propia. Responde 409.
 */
public class TransactionNotEditableException extends RuntimeException {

  public TransactionNotEditableException(TransactionId transactionId, String reason) {
    super("La transacción " + transactionId.value() + " no se puede editar: " + reason);
  }
}
