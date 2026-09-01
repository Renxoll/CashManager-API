package pe.smartcash.cash.groups.domain.services;

import java.math.BigDecimal;
import java.time.Instant;
import pe.smartcash.cash.groups.domain.model.valueobjects.SettlementId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

public record SettlementDetail(
    SettlementId settlementId,
    UserId fromUserId,
    String fromDisplayName,
    UserId toUserId,
    String toDisplayName,
    BigDecimal amount,
    String currency,
    Instant createdAt) {}
