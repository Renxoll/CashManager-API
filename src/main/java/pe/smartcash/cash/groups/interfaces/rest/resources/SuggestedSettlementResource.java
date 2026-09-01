package pe.smartcash.cash.groups.interfaces.rest.resources;

import java.math.BigDecimal;
import java.util.UUID;

public record SuggestedSettlementResource(
    UUID fromUserId, String fromDisplayName, UUID toUserId, String toDisplayName, BigDecimal amount, String currency) {}
