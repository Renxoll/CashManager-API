package pe.smartcash.cash.groups.domain.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import pe.smartcash.cash.groups.domain.model.valueobjects.ExpenseId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

public record ExpenseDetail(
    ExpenseId expenseId,
    String description,
    BigDecimal amount,
    String currency,
    UserId paidByUserId,
    String paidByDisplayName,
    Instant createdAt,
    List<ExpenseShareDetail> shares) {}
