package pe.smartcash.cash.groups.interfaces.rest.resources;

import java.math.BigDecimal;
import java.util.UUID;

public record ExpenseShareResource(UUID userId, String displayName, BigDecimal amount) {}
