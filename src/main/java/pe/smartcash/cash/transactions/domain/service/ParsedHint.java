package pe.smartcash.cash.transactions.domain.service;

import pe.smartcash.cash.transactions.domain.model.valueobjects.Merchant;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Money;

public record ParsedHint(Money money, Merchant merchant) {}
