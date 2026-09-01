package pe.smartcash.cash.groups.domain.services;

import java.math.BigDecimal;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

public record ExpenseShareDetail(UserId userId, String displayName, BigDecimal amount) {}
