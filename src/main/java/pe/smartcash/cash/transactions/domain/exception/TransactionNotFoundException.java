package pe.smartcash.cash.transactions.domain.exception;

import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;

/**
 * La transacción no existe, o existe pero no pertenece al usuario autenticado -- ambos casos
 * responden 404, nunca 403: un cliente no debe poder distinguir "no existe" de "no es tuya"
 * probando ids ajenos.
 */
public class TransactionNotFoundException extends RuntimeException {

  public TransactionNotFoundException(TransactionId transactionId) {
    super("Transacción no encontrada: " + transactionId.value());
  }
}
