package pe.smartcash.cash.transactions.domain.services;

import pe.smartcash.cash.transactions.domain.model.valueobjects.Merchant;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Money;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionStatus;

/** Read-model devuelto por {@link TransactionQueryService}: ya trae la categoría resuelta. */
public record TransactionDetail(
    TransactionId id, TransactionStatus status, Money money, Merchant merchant, CategoryDescriptor category, String errorMessage) {}
