package pe.smartcash.cash.transactions.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResource(
    UUID transactionId,
    String status,
    BigDecimal amount,
    String currency,
    String merchant,
    String categoryCode,
    String category,
    Instant createdAt) {}
