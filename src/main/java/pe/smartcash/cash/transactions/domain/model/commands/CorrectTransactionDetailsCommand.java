package pe.smartcash.cash.transactions.domain.model.commands;

import java.math.BigDecimal;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

/**
 * El usuario completa a mano un gasto que Luki no pudo leer (FAILED) o corrige el monto y el
 * comercio de uno ya leído (p. ej. "Comercio desconocido"). {@code categoryCode} es
 * obligatorio solo para completar un FAILED; en un gasto ya PROCESSED se ignora (la categoría
 * se cambia por su propio endpoint).
 */
public record CorrectTransactionDetailsCommand(
    TransactionId transactionId, UserId requestingUserId, BigDecimal amount, String currency, String merchant, String categoryCode) {}
