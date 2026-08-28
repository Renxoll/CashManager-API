package pe.smartcash.cash.transactions.domain.model.commands;

import java.math.BigDecimal;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

/** {@code source}: quién envió el dinero o de dónde vino (ej. "Sueldo", "Juan Pérez") -- lo
 * que el usuario haya escrito, se guarda tal cual como {@code merchant} de la transacción. */
public record RecordManualIncomeCommand(UserId userId, BigDecimal amount, String currency, String source) {}
