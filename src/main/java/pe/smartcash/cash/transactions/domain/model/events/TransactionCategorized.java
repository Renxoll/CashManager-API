package pe.smartcash.cash.transactions.domain.model.events;

import java.time.Instant;
import pe.smartcash.cash.transactions.domain.model.valueobjects.CategoryCode;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Merchant;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Money;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

/** Hecho de negocio: una transacción recién recibida terminó de categorizarse con éxito. */
public record TransactionCategorized(
    TransactionId transactionId,
    UserId userId,
    Money money,
    Merchant merchant,
    CategoryCode categoryCode,
    Instant occurredAt) {}
