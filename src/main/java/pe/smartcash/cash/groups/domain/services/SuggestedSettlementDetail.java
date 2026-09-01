package pe.smartcash.cash.groups.domain.services;

import java.math.BigDecimal;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

public record SuggestedSettlementDetail(
    UserId fromUserId, String fromDisplayName, UserId toUserId, String toDisplayName, BigDecimal amount, String currency) {}
