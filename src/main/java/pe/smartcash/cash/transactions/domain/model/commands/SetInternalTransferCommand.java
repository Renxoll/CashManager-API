package pe.smartcash.cash.transactions.domain.model.commands;

import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

/**
 * El usuario marca ({@code internalTransfer = true}) o desmarca una transacción como
 * movimiento entre sus propias cuentas. {@code requestingUserId} se valida contra el dueño
 * real: si no coincide, mismo 404 que si el id no existiera.
 */
public record SetInternalTransferCommand(TransactionId transactionId, UserId requestingUserId, boolean internalTransfer) {}
