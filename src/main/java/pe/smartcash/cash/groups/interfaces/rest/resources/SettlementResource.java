package pe.smartcash.cash.groups.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SettlementResource(
    UUID settlementId,
    UUID fromUserId,
    String fromDisplayName,
    UUID toUserId,
    String toDisplayName,
    BigDecimal amount,
    String currency,
    Instant createdAt) {}
