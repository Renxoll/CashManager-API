package pe.smartcash.cash.transactions.domain.services;

import pe.smartcash.cash.transactions.domain.model.valueobjects.CategoryCode;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Merchant;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Money;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionType;

public record ExtractionResult(Money money, Merchant merchant, CategoryCode categoryCode, TransactionType type) {}
