package pe.smartcash.cash.transactions.domain.services;

import java.time.Instant;
import java.util.UUID;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Merchant;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Money;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionStatus;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionType;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

/** Read-model devuelto por {@link TransactionQueryService}: ya trae la categoría y el módulo resueltos. */
public record TransactionDetail(
    TransactionId id,
    UserId userId,
    TransactionStatus status,
    Money money,
    Merchant merchant,
    ResolvedCategory category,
    TransactionType type,
    boolean internalTransfer,
    String errorMessage,
    Instant createdAt,
    UUID workspaceId) {}
