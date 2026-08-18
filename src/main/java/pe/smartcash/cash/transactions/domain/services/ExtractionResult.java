package pe.smartcash.cash.transactions.domain.services;

import pe.smartcash.cash.transactions.domain.model.valueobjects.CategoryCode;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Merchant;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Money;

public record ExtractionResult(Money money, Merchant merchant, CategoryCode categoryCode) {}
