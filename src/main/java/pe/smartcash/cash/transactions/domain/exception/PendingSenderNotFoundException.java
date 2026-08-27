package pe.smartcash.cash.transactions.domain.exception;

import pe.smartcash.cash.transactions.domain.model.valueobjects.PendingSenderId;

/**
 * No existe, o existe pero no pertenece al usuario autenticado -- mismo 404 en ambos casos,
 * mismo criterio que {@code TransactionNotFoundException}.
 */
public class PendingSenderNotFoundException extends RuntimeException {

  public PendingSenderNotFoundException(PendingSenderId pendingSenderId) {
    super("Remitente pendiente no encontrado: " + pendingSenderId.value());
  }
}
