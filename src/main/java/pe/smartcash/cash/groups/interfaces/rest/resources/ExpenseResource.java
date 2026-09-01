package pe.smartcash.cash.groups.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExpenseResource(
    UUID expenseId,
    String description,
    BigDecimal amount,
    String currency,
    UUID paidByUserId,
    String paidByDisplayName,
    Instant createdAt,
    List<ExpenseShareResource> shares) {}
